// Builds a plane carrying earth, life, taint and motion - the full stage-2 stack: layered terrain, caves, pockets, the
// tainted biome and the plane's effects - and moves the player inside it for a screenshot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level";
}

var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var victus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:victus");
var vitium = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:vitium");
var motus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:motus");
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(terra, 40);
aspects.put(victus, 20);
aspects.put(vitium, 20);
aspects.put(motus, 20);

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Screenshot Plane", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
sb.append("id=").append(id).append(" centre=").append(centre).append(" radius=").append(
        fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects));

// Stand on the pad's corner, looking back at the doorway.
var arrival = centre.offset(2, 1, 2);
var moved = fr.wind_blade.isorropia.common.world.PocketPlane.sendTo(player, pocket,
        new net.minecraft.world.phys.Vec3(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5));
sb.append(" moved=").append(moved != null);
if (moved != null) {
    sb.append(" playerAt=").append(moved.blockPosition());
}
return sb.toString();
