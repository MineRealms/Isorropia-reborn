package fr.wind_blade.isorropia.common.world;

import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.tiles.TilePortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * TH's {@code PocketPlaneData.makePortal} / {@code destroyPortal} / {@code firstAvailablePortal}, the paired-gateway
 * bookkeeping: a plane offers four sides, each of which can hold one gateway whose other end stands in the world the
 * visitor arrived from.
 *
 * <p>TH carved the plane-side portal with {@code setBlock} and left the tile's {@code dimension}/{@code pocket} fields
 * to code the decompiler dropped; this port fills them explicitly and also stamps the arrival position, so a portal
 * never has to derive where it leads from where it stands.</p>
 */
public final class PocketGateways {
    /** TH's four sides, in its own order: 1 +Z, 2 -Z, 3 +X, 4 -X of the plane's centre column. */
    public static final int SIDES = 4;

    private PocketGateways() {
    }

    /** TH's firstAvailablePortal: the first of the four sides with no gateway, or 0 when the plane is full. */
    public static int firstAvailablePortal(PocketPlaneStore store, int planeId) {
        PocketPlaneStore.Plane plane = store.plane(planeId);
        return plane == null ? 0 : plane.firstFreePortal();
    }

    /**
     * Records the gateway on side {@code which} and carves its twin into the plane's edge.
     *
     * @return true when the side was free and both ends now exist.
     */
    public static boolean makePortal(ServerLevel pocket, PocketPlaneStore store, int planeId, int which, int x, int y,
            int z, ResourceKey<Level> returnDimension) {
        if (which < 1 || which > SIDES) {
            return false;
        }
        PocketPlaneStore.Plane plane = store.plane(planeId);
        if (plane == null || plane.portalPos(which) != null) {
            return false;
        }
        BlockPos arrival = new BlockPos(x, y, z);
        store.setPortal(planeId, which, arrival, returnDimension);
        carvePlanePortal(pocket, planeId, plane.radius(), which, arrival, returnDimension);
        return true;
    }

    /** Clears the slot and puts the plane's shell back where the portal was. */
    public static void destroyPortal(ServerLevel pocket, PocketPlaneStore store, int planeId, int which) {
        PocketPlaneStore.Plane plane = store.plane(planeId);
        if (plane == null || plane.portalPos(which) == null) {
            return;
        }
        BlockPos twin = twinPos(planeId, plane.radius(), which);
        if (twin != null) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        PocketPlaneTerrain.setFast(pocket, twin.getX() + dx, twin.getY() + dy, twin.getZ() + dz,
                                BlocksIS.VOID.get().defaultBlockState());
                    }
                }
            }
        }
        store.setPortal(planeId, which, null, null);
    }

    /** Where the plane-side portal of side {@code which} stands. */
    @Nullable
    public static BlockPos twinPos(int planeId, int radius, int which) {
        BlockPos centre = PocketPlane.anchor(planeId);
        return switch (which) {
            case 1 -> centre.offset(0, 0, radius);
            case 2 -> centre.offset(0, 0, -radius);
            case 3 -> centre.offset(radius, 0, 0);
            case 4 -> centre.offset(-radius, 0, 0);
            default -> null;
        };
    }

    /** Which side a plane-side portal at {@code pos} belongs to, or 0 if it is not one. */
    public static int sideOf(int planeId, int radius, BlockPos pos) {
        for (int which = 1; which <= SIDES; which++) {
            if (twinPos(planeId, radius, which).equals(pos)) {
                return which;
            }
        }
        return 0;
    }

    /** TH's plane-side carve: a three-by-three portal block with a void block behind it. */
    private static void carvePlanePortal(ServerLevel pocket, int planeId, int radius, int which, BlockPos arrival,
            ResourceKey<Level> returnDimension) {
        BlockPos centre = PocketPlane.anchor(planeId);
        BlockPos twin = twinPos(planeId, radius, which);
        Direction outward = outwardOf(which);
        BlockState portal = BlocksIS.PORTAL.get().defaultBlockState();
        BlockState shell = BlocksIS.VOID.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos cell = twin.offset(perpendicular(which, dx), dy, perpendicular(which, -dx));
                PocketPlaneTerrain.setFast(pocket, cell.getX(), cell.getY(), cell.getZ(), portal);
                PocketPlaneTerrain.setFast(pocket, cell.getX() + outward.getStepX(), cell.getY() + outward.getStepY(),
                        cell.getZ() + outward.getStepZ(), shell);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos cell = twin.offset(perpendicular(which, dx), dy, perpendicular(which, -dx));
                if (pocket.getBlockEntity(cell) instanceof TilePortal tile) {
                    tile.link(returnDimension, arrival);
                }
            }
        }
        // The centre column keeps its landing pad clear.
        PocketPlaneTerrain.setFast(pocket, centre.getX(), centre.getY() + 1, centre.getZ(),
                Blocks.AIR.defaultBlockState());
    }

    private static Direction outwardOf(int which) {
        return switch (which) {
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.WEST;
        };
    }

    /** The in-plane offset for one step across the portal, perpendicular to its outward direction. */
    private static BlockPos perpendicular(int which, int step) {
        return switch (which) {
            case 1, 2 -> new BlockPos(step, 0, 0);
            default -> new BlockPos(0, 0, step);
        };
    }
}
