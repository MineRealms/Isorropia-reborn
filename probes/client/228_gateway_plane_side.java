// Stage 3, part 3: put the player inside plane 13 in front of the plane-side portal, so the swirl's destination tint
// can be photographed. The world-side gateway was cut by probe 226.
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
var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (plane == null || pocket == null) {
    return "no_plane";
}
var twin = fr.wind_blade.isorropia.common.world.PocketGateways.twinPos(id, plane.radius(), which);
sb.append("plane=").append(id).append(" which=").append(which).append(" twin=").append(twin).append("\n");

// Stand three blocks in front of the plane-side opening, inside the plane, facing it.
var stand = new net.minecraft.core.BlockPos(twin.getX(), twin.getY(), twin.getZ() - 3);
pocket.getChunk(twin.getX() >> 4, twin.getZ() >> 4);
var moved = fr.wind_blade.isorropia.common.world.PocketPlane.sendTo(player, pocket,
        new net.minecraft.world.phys.Vec3(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5), 0.0F);
sb.append("moved=").append(moved != null).append(" at=").append(player.blockPosition())
        .append(" dim=").append(player.level().dimension().location());
return sb.toString();
