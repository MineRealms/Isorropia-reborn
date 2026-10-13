package fr.wind_blade.isorropia.common.world;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Thaumic Horizons' pocket plane, reduced to the parts that do not depend on terrain generation. The dimension itself
 * is a datapack dimension resting on a void {@code minecraft:flat} generator, exactly as TC4R registers its own outer
 * lands; TH never generated the pocket world chunk-by-chunk either, it stamped a self-contained island into the void
 * (see {@link PocketPlaneStore} for the plane registry, and the terrain stage for the stamping itself).
 *
 * <p>TH packed one plane per 256-block column along +Z, each centred at {@code (0, 128, 256 * id)} with a radius of at
 * most 127, so the columns never touch. The exit rift sits one block above the anchor.</p>
 */
public final class PocketPlane {
    public static final ResourceKey<Level> LEVEL =
            ResourceKey.create(Registries.DIMENSION, Isorropia.id("pocket_plane"));
    public static final int COLUMN_SPACING = 256;
    public static final int CENTER_Y = 128;

    private PocketPlane() {
    }

    /** The middle of the column that holds plane {@code id}. */
    public static BlockPos anchor(int id) {
        return new BlockPos(0, CENTER_Y, id * COLUMN_SPACING);
    }

    /** The rift a visitor leaves through, directly above the anchor as TH's was. */
    public static BlockPos exit(int id) {
        return anchor(id).above();
    }

    @Nullable
    public static ServerLevel level(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(LEVEL);
    }

    /** Moves an entity any distance, keeping it loaded for a moment on arrival. Copied from TC4R's eldritch portal. */
    @Nullable
    public static Entity sendTo(Entity entity, ServerLevel destination, Vec3 arrival) {
        Entity moved = entity.changeDimension(destination, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity traveller, ServerLevel level,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(arrival, Vec3.ZERO, traveller.getYRot(), traveller.getXRot());
            }

            @Override
            public Entity placeEntity(Entity traveller, ServerLevel oldLevel, ServerLevel newLevel, float yaw,
                    Function<Boolean, Entity> repositionEntity) {
                Entity placed = repositionEntity.apply(false);
                placed.moveTo(arrival.x, arrival.y, arrival.z, yaw, traveller.getXRot());
                return placed;
            }
        });
        if (moved != null) {
            destination.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(BlockPos.containing(arrival)), 3,
                    BlockPos.containing(arrival));
        }
        return moved;
    }

    /** Sends a player to the given plane's rift. */
    public static boolean enterPlane(ServerPlayer player, int planeId) {
        ServerLevel destination = level(player.server);
        if (destination == null) {
            return false;
        }
        BlockPos arrival = exit(planeId);
        return sendTo(player, destination, new Vec3(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5)) != null;
    }
}
