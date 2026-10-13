package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.tiles.TilePortal;
import fr.wind_blade.isorropia.common.tiles.TileSlot;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import fr.wind_blade.isorropia.common.world.PocketGateways;
import fr.wind_blade.isorropia.common.world.PocketPlane;
import fr.wind_blade.isorropia.common.world.PocketPlaneStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The surface of a planar gateway, Thaumic Horizons' {@code BlockPortalTH}. It is unbreakable, has no collision and
 * draws itself entirely through its block entity renderer; walking into it is what moves you.
 *
 * <p>Both directions are one hook, exactly as in TH. A portal that knows its destination is the plane-side twin, and
 * sends the visitor to the receptacle that opened it. A portal that does not is the world-side surface, and it walks
 * up its own frame to the keystone receptacle above it to learn which plane and which of the plane's four sides it
 * stands for. TH used a single block for both ends too; here the destination simply travels in the tile instead of
 * being looked up by position.</p>
 */
public class BlockPortal extends Block implements EntityBlock {
    public BlockPortal(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePortal(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TilesIS.PORTAL.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, p, s, be) -> TilePortal.clientTick(lvl, p, s, (TilePortal) be)
                : (lvl, p, s, be) -> TilePortal.serverTick(lvl, p, s, (TilePortal) be);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.isPassenger() || player.isVehicle()) {
            return;
        }
        // TH gave the traveller a short immunity on both entry and exit so a portal never bounces them back. It has
        // to decay rather than be refreshed here: the arrival lands inside the far opening, and a refreshed timer
        // would pin the visitor in the surface forever instead of letting them walk out.
        if (player.getPortalCooldown() > 0) {
            return;
        }
        player.setPortalCooldown(PocketPlane.PORTAL_IMMUNITY_TICKS);
        if (!(level.getBlockEntity(pos) instanceof TilePortal portal)) {
            return;
        }
        Vec3 landing = portal.landingSpot();
        if (landing != null && portal.destination() != null) {
            ServerLevel target = player.server.getLevel(portal.destination());
            if (target != null) {
                PocketPlane.sendTo(player, target, landing);
            }
            return;
        }
        travelToPlane(level, pos, player);
    }

    /**
     * TH's world-side route: climb the column of portal blocks to the frame row, step sideways to the receptacle, and
     * let it say which plane and side this gateway serves.
     */
    private static void travelToPlane(Level level, BlockPos pos, ServerPlayer player) {
        int slotX = pos.getX();
        int slotY = pos.getY();
        int slotZ = pos.getZ();
        do {
            slotY++;
        } while (level.getBlockState(new BlockPos(slotX, slotY, slotZ)).is(BlocksIS.PORTAL.get()));
        BlockPos frame = new BlockPos(slotX, slotY, slotZ);
        if (level.getBlockState(frame).is(BlocksIS.GATEWAY.get())) {
            if (level.getBlockState(frame.east()).is(BlocksIS.SLOT.get())) {
                slotX++;
            } else if (level.getBlockState(frame.west()).is(BlocksIS.SLOT.get())) {
                slotX--;
            } else if (level.getBlockState(frame.south()).is(BlocksIS.SLOT.get())) {
                slotZ++;
            } else if (level.getBlockState(frame.north()).is(BlocksIS.SLOT.get())) {
                slotZ--;
            }
        }
        if (!(level.getBlockEntity(new BlockPos(slotX, slotY, slotZ)) instanceof TileSlot slot)
                || !slot.portalOpen() || slot.planeId() < 0) {
            return;
        }
        ServerLevel pocket = PocketPlane.level(player.server);
        if (pocket == null) {
            return;
        }
        PocketPlaneStore.Plane plane = PocketPlaneStore.get(player.server).plane(slot.planeId());
        if (plane == null) {
            return;
        }
        BlockPos twin = PocketGateways.twinPos(slot.planeId(), plane.radius(), slot.which());
        if (twin == null) {
            return;
        }
        float yaw = switch (slot.which()) {
            case 1 -> 180.0F;
            case 3 -> 90.0F;
            case 4 -> 270.0F;
            default -> 0.0F;
        };
        PocketPlane.sendTo(player, pocket, new Vec3(twin.getX() + 0.5, twin.getY(), twin.getZ() + 0.5), yaw);
    }

    /** TH's portal sparkle: the drift runs across the face of the plane, whichever way the gateway is turned. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean facesX = !level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this);
        for (int i = 0; i < 4; i++) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble();
            double pz = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = (random.nextDouble() - 0.5) * 0.5;
            double vz = (random.nextDouble() - 0.5) * 0.5;
            int sign = random.nextInt(2) * 2 - 1;
            if (facesX) {
                px = pos.getX() + 0.5 + 0.25 * sign;
                vx = random.nextDouble() * 2.0 * sign;
            } else {
                pz = pos.getZ() + 0.5 + 0.25 * sign;
                vz = random.nextDouble() * 2.0 * sign;
            }
            level.addParticle(ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
        }
    }
}
