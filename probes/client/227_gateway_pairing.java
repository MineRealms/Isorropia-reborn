// Stage 3, part 2: round-trip through the gateway cut by probe 226. Walks the player into the world-side portal,
// asserts arrival on the plane-side twin, then steps into the plane-side portal and asserts the return to the
// receptacle's landing spot in the overworld.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var slotPos = new net.minecraft.core.BlockPos(100, 200, 100);
if (!(level.getBlockEntity(slotPos) instanceof fr.wind_blade.isorropia.common.tiles.TileSlot slot)
        || !slot.portalOpen()) {
    return "no_open_gateway";
}
int id = slot.planeId();
int which = slot.which();
var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
var plane = store.plane(id);
if (plane == null) {
    return "no_plane";
}
int radius = plane.radius();
var twin = fr.wind_blade.isorropia.common.world.PocketGateways.twinPos(id, radius, which);
var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (twin == null || pocket == null) {
    return "no_twin";
}
sb.append("plane=").append(id).append(" which=").append(which).append(" twin=").append(twin).append("\n");

// Keep both ends loaded across the move; a changeDimension needs its destination chunk resident.
level.getChunk(slotPos.getX() >> 4, slotPos.getZ() >> 4);
pocket.getChunk(twin.getX() >> 4, twin.getZ() >> 4);

// World to plane: stand in a cell of the three-by-three opening and let the block's collision hook run.
var portalPos = new net.minecraft.core.BlockPos(100, 198, 100);
var portalState = level.getBlockState(portalPos);
sb.append("worldPortalIsPortal=")
        .append(portalState.is(fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get())).append("\n");
player.setPortalCooldown(0);
player.teleportTo(level, 100.5, 198.0, 100.5, 0.0F, 0.0F);
fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get().entityInside(portalState, level, portalPos, player);
sb.append("afterWorld dim=").append(player.level().dimension().location())
        .append(" pos=").append(player.blockPosition())
        .append(" immunity=").append(player.getPortalCooldown()).append("\n");

// Plane to world: the twin knows where it leads, so the same hook sends the visitor home.
if (pocket.getBlockEntity(twin) instanceof fr.wind_blade.isorropia.common.tiles.TilePortal tp
        && tp.landingSpot() != null) {
    sb.append("landingSpot=").append(tp.landingSpot()).append("\n");
    player.setPortalCooldown(0);
    var planeState = pocket.getBlockState(twin);
    fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get().entityInside(planeState, pocket, twin, player);
    sb.append("afterPlane dim=").append(player.level().dimension().location())
            .append(" pos=").append(player.blockPosition())
            .append(" immunity=").append(player.getPortalCooldown()).append("\n");
    // The arrival is inside the far opening, so step out of it the way a visitor would.
    player.teleportTo(level, 100.5, 198.0, 103.5, 0.0F, 0.0F);
    sb.append("steppedOut=").append(player.blockPosition());
} else {
    sb.append("planeTile=missing\n");
}
return sb.toString();
