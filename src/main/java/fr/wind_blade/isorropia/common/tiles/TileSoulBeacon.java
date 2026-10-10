package fr.wind_blade.isorropia.common.tiles;

import fr.wind_blade.isorropia.common.sounds.SoundsIS;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The soul beacon a wizard ties their reincarnation to. Its whole visible behaviour is the beam it throws into
 * the sky, which grows while the beacon is on screen exactly as a vanilla beacon's does.
 *
 * <p>Binding a player to the beacon is the reincarnation ritual; the beacon only records where.
 */
public class TileSoulBeacon extends BlockEntity {
    /** The names the vat and the death handler read back. */
    public static final String BOUND = "soulBeacon";
    public static final String COORDS = "soulBeaconCoords";
    public static final String DIMENSION = "soulBeaconDim";

    private float beamScale;
    private long lastGameTime;

    public TileSoulBeacon(BlockPos pos, BlockState state) {
        super(TilesIS.SOUL_BEACON.get(), pos, state);
    }

    /** Ties a player to this beacon, which is where they will come back to. */
    public boolean activate(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        data.putBoolean(BOUND, true);
        data.putIntArray(COORDS, new int[] {worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()});
        data.putInt(DIMENSION, server.dimension().location().hashCode());
        player.displayClientMessage(Component.translatable("isorropia.setBeacon").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC), false);
        level.playSound(null, worldPosition, SoundsIS.CURATIVE_INFUSION_START.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        return true;
    }

    /**
     * The beam's length factor, animated the way vanilla's beacon animates it: it climbs a little every frame it
     * is drawn and falls back when frames were skipped, so it fades in when a player looks at the beacon.
     */
    public float beamScale() {
        if (level == null) {
            return 0.0F;
        }
        long now = level.getGameTime();
        long elapsed = now - lastGameTime;
        lastGameTime = now;
        if (elapsed > 1) {
            beamScale -= elapsed / 40.0F;
            if (beamScale < 0.0F) {
                beamScale = 0.0F;
            }
        }
        beamScale += 0.025F;
        if (beamScale > 1.0F) {
            beamScale = 1.0F;
        }
        return beamScale;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0.0, 256.0, 0.0).inflate(1.0);
    }
}
