package fr.wind_blade.isorropia.common.tiles;

import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectContainerView;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thaumic Horizons' vortex, ported to TC4R. It warms up for fifty ticks, then swallows the 5x5x5 around itself and
 * turns on the surrounding world: anything alive within fifteen blocks is dragged toward the rift and torn apart at
 * point-blank range, and up to three nearby soft blocks a tick are ground away. An attenuator's beam damps all of this
 * ({@link #beams} up to six); TH opened a pocket plane once fully damped, which waits on the dimension port.
 *
 * <p>TH's void crafting (void putty, crystal wand, golem powder), the pocket plane and its teleporters are not ported
 * yet, so the item hopper and the dimensional hand-off are absent; the machine is otherwise faithful.</p>
 */
public class TileVortex extends BlockEntity implements AspectContainerView {
    /** TH's collapse trigger: the rift tears itself apart once it has eaten this long. */
    private static final int MAX_COUNT = 2400;
    /** Past this many beams the rift stops eating entirely. */
    private static final int MAX_BEAMS = 6;

    private int count;
    /** How many vortex attenuators are currently holding this rift open. */
    private int beams;
    private boolean ateDevices;
    private boolean collapsing;
    /**
     * TH's cheat rift: it never eats, never grinds and never collapses. A pocket plane's exit rift is one of these, so
     * each island keeps its doorway (and TH's creative-only variant was one too).
     */
    private boolean cheat;
    private final Map<AspectId, Integer> aspects = new LinkedHashMap<>();
    private final List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
    /** Last beam count pushed to clients, so a same-tick change cannot send two packets. */
    private int lastSyncedBeams = -1;

    public TileVortex(BlockPos pos, BlockState state) {
        super(TilesIS.VORTEX.get(), pos, state);
    }

    public int count() {
        return count;
    }

    public int beams() {
        return beams;
    }

    public boolean collapsing() {
        return collapsing;
    }

    public boolean isCheat() {
        return cheat;
    }

    public void setCheat(boolean value) {
        this.cheat = value;
        syncToClients();
    }

    /** Read-only aspect view, ordered as TH's AspectList was. */
    public Map<AspectId, Integer> aspectMap() {
        return java.util.Collections.unmodifiableMap(aspects);
    }

    public List<net.minecraft.world.item.ItemStack> storedItems() {
        return List.copyOf(items);
    }

    public void setBeams(int value) {
        if (this.beams != value) {
            this.beams = value;
            syncToClients();
        }
    }

    /** Fills the rift with a random handful of aspects, as TH's {@code onBlockPlacedBy} did. */
    public void rollAspects(RandomSource random) {
        aspects.clear();
        List<AspectId> primals = List.copyOf(AspectApi.primals());
        List<AspectId> compounds = List.copyOf(AspectApi.registry().compounds());
        int chosen = random.nextInt(4) + 1;
        for (int a = 0; a < chosen; a++) {
            List<AspectId> pool = random.nextInt(3) == 0 && !compounds.isEmpty() ? compounds : primals;
            if (pool.isEmpty()) {
                continue;
            }
            aspects.merge(pool.get(random.nextInt(pool.size())), random.nextInt(30), Integer::sum);
        }
        syncToClients();
    }

    /** Replaces the rift's aspects wholesale, as the amplifier catastrophe does when a node collapses into one. */
    public void setAspects(Map<AspectId, Integer> source) {
        aspects.clear();
        aspects.putAll(source);
        syncToClients();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileVortex vortex) {
        if (vortex.beams != vortex.lastSyncedBeams) {
            vortex.syncToClients();
            vortex.lastSyncedBeams = vortex.beams;
        }
        if (vortex.collapsing) {
            vortex.count++;
            if (vortex.count > 25) {
                level.removeBlock(pos, false);
                collapse(level, pos);
            }
            return;
        }
        if (vortex.count < 50) {
            vortex.count++;
            return;
        }
        if (vortex.cheat) {
            // A doorway, not a predator: nothing to eat, nothing to grind, no clock running down.
            return;
        }
        if (!vortex.ateDevices) {
            // TH cleared the whole neighbourhood the moment the rift matured.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            level.removeBlock(pos.offset(dx, dy, dz), false);
                        }
                    }
                }
            }
            vortex.ateDevices = true;
        }
        if (vortex.beams < MAX_BEAMS) {
            vortex.pullEntities(level, pos);
            vortex.grindBlocks(level, pos);
        }
        // (TH opened the pocket plane once fully damped; that half waits on the dimension port.)
        vortex.count += Math.max(0, MAX_BEAMS - vortex.beams);
        if (vortex.count > MAX_COUNT) {
            vortex.collapsing = true;
            vortex.count = 0;
            vortex.syncToClients();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileVortex vortex) {
        // Nothing to advance; the renderer works off the synced fields and the wall clock.
    }

    /** TH's handleHungryNode entity half: drag in everything within fifteen blocks and maul what gets close. */
    private void pullEntities(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(15.0);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
            if (entity instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            if (entity.isAlive() && !entity.isInvulnerable()
                    && entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 2.0) {
                if (entity instanceof FallingBlockEntity) {
                    entity.discard();
                } else {
                    entity.hurt(level.damageSources().fellOutOfWorld(), 3.0F - this.beams / 2.0F);
                }
            }
            double dx = (pos.getX() + 0.5 - entity.getX()) / 15.0;
            double dy = (pos.getY() + 0.5 - entity.getY()) / 15.0;
            double dz = (pos.getZ() + 0.5 - entity.getZ()) / 15.0;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double falloff = 1.0 - distance;
            double modifier = 2.0 - this.beams / 3.0;
            if (falloff <= 0.0 || distance == 0.0) {
                continue;
            }
            falloff *= falloff;
            entity.push(dx / distance * falloff * 0.15 * modifier,
                    dy / distance * falloff * 0.25 * modifier,
                    dz / distance * falloff * 0.15 * modifier);
        }
    }

    /** TH's handleHungryNode block half: three attempts a tick to chew through a nearby soft block. */
    private void grindBlocks(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            // Damped rifts only get through occasionally.
            if (this.beams > 0 && random.nextInt(Math.max(1, this.beams)) != 0) {
                continue;
            }
            int tx = pos.getX() + random.nextInt(16) - random.nextInt(16);
            int ty = pos.getY() + random.nextInt(16) - random.nextInt(16);
            int tz = pos.getZ() + random.nextInt(16) - random.nextInt(16);
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, tx, tz);
            if (ty > surface) {
                ty = surface;
            }
            Vec3 from = Vec3.atCenterOf(pos);
            Vec3 to = new Vec3(tx + 0.5, ty + 0.5, tz + 0.5);
            BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(from, to,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                    null));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().distSqr(pos) >= 256.0) {
                continue;
            }
            BlockPos target = hit.getBlockPos();
            BlockState targetState = level.getBlockState(target);
            float hardness = targetState.getDestroySpeed(level, target);
            if (hardness >= 0.0F && hardness < 10.0F) {
                level.destroyBlock(target, true);
            }
        }
    }

    /** TH's collapse: the rift vanishes in a blast and seeds flux over whatever it was holding. */
    private static void collapse(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F,
                Level.ExplosionInteraction.BLOCK);
        for (int attempt = 0; attempt < 50; attempt++) {
            RandomSource random = server.getRandom();
            BlockPos target = pos.offset(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(8) - random.nextInt(8), random.nextInt(8) - random.nextInt(8));
            if (server.getBlockState(target).isAir()) {
                var fluid = target.getY() < pos.getY()
                        ? dev.tc4port.thaumcraft.registry.TCFluids.FLUX_GOO.get()
                        : dev.tc4port.thaumcraft.registry.TCFluids.FLUX_GAS.get();
                dev.tc4port.thaumcraft.api.flux.FluxApi.place(server, target, fluid, 9, 3);
            }
        }
    }

    @Override
    public AspectAmounts visibleAspects() {
        return new AspectAmounts(aspects);
    }

    private void syncToClients() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("count", this.count);
        tag.putInt("beams", this.beams);
        tag.putBoolean("ateDevices", this.ateDevices);
        tag.putBoolean("collapsing", this.collapsing);
        tag.putBoolean("cheat", this.cheat);
        ListTag aspectTags = new ListTag();
        for (Map.Entry<AspectId, Integer> entry : this.aspects.entrySet()) {
            CompoundTag aspectTag = new CompoundTag();
            aspectTag.putString("key", entry.getKey().serialized());
            aspectTag.putInt("amount", entry.getValue());
            aspectTags.add(aspectTag);
        }
        tag.put("aspects", aspectTags);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.count = tag.getInt("count");
        this.beams = tag.getInt("beams");
        this.ateDevices = tag.getBoolean("ateDevices");
        this.collapsing = tag.getBoolean("collapsing");
        this.cheat = tag.getBoolean("cheat");
        this.lastSyncedBeams = this.beams;
        this.aspects.clear();
        ListTag aspectTags = tag.getList("aspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < aspectTags.size(); i++) {
            CompoundTag aspectTag = aspectTags.getCompound(i);
            AspectId id = AspectId.parse(aspectTag.getString("key"));
            if (id != null) {
                this.aspects.put(id, aspectTag.getInt("amount"));
            }
        }
    }
}
