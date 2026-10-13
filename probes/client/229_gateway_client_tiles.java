// Stage 3, part 4: the nine cells of a gateway must reach the client as portal blocks with their tiles, or the swirl
// never draws and nothing can be walked into. Checked on whichever end the client is standing on.
var mc = net.minecraft.client.Minecraft.getInstance();
var clientLevel = mc.level;
var server = mc.getSingleplayerServer();
var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
var slot = (fr.wind_blade.isorropia.common.tiles.TileSlot) server.overworld()
        .getBlockEntity(new net.minecraft.core.BlockPos(100, 200, 100));
if (slot == null || !slot.portalOpen()) {
    return "no_open_gateway";
}
var plane = store.plane(slot.planeId());
var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
var twin = fr.wind_blade.isorropia.common.world.PocketGateways.twinPos(slot.planeId(), plane.radius(),
        slot.which());
// The world-side surface is the three-by-three opening below the receptacle; the plane-side one is the twin.
boolean atHome = clientLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
var centre = atHome ? new net.minecraft.core.BlockPos(100, 198, 100) : twin;
int portals = 0;
int tiles = 0;
int open = 0;
for (int dx = -1; dx <= 1; dx++) {
    for (int dy = -1; dy <= 1; dy++) {
        var pos = centre.offset(dx, dy, 0);
        if (clientLevel.getBlockState(pos).is(fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get())) {
            portals++;
        }
        if (clientLevel.getBlockEntity(pos) instanceof fr.wind_blade.isorropia.common.tiles.TilePortal tp) {
            tiles++;
            if (tp.openCount() >= 30) {
                open++;
            }
        }
    }
}
return "clientDim=" + clientLevel.dimension().location() + " side=" + (atHome ? "world" : "plane")
        + " centre=" + centre + " portals=" + portals + " tiles=" + tiles + " fullyOpen=" + open
        + " pocket=" + (pocket != null);
