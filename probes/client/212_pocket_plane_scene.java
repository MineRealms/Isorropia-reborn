// Builds a pocket plane and moves the player inside it, so a screenshot can show the island from within.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level";
}

var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var vitreus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:vitreus");
var tenebrae = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:tenebrae");
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(aer, 60);
aspects.put(vitreus, 20);
aspects.put(tenebrae, 20);

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Screenshot Island",
        aspects, net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
sb.append("id=").append(id).append(" centre=").append(centre).append(" radius=").append(
        fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects));

// Stand on the pad's edge, a few blocks back from the doorway, and look at it.
var arrival = centre.offset(6, 1, 0);
var moved = fr.wind_blade.isorropia.common.world.PocketPlane.sendTo(player, pocket,
        new net.minecraft.world.phys.Vec3(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5));
sb.append(" moved=").append(moved != null);
if (moved != null) {
    sb.append(" playerAt=").append(moved.blockPosition());
}
return sb.toString();
