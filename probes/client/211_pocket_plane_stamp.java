// Pocket plane stage 2a: stamping the frame of a plane. Checks the radius/colour maths, the aspect ring, the void shell,
// the landing pad and the cheat exit rift, plus the registry entry.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level\n";
}

var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var tenebrae = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:tenebrae");
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(aer, 43);
aspects.put(tenebrae, 1);

sb.append("radius=").append(fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects)).append("\n");
sb.append("colour=").append(Integer.toHexString(
        fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.colourOf(aspects))).append("\n");

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Probe Island", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
sb.append("id=").append(id).append("\n");
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
sb.append("centre=").append(centre).append("\n");

sb.append("shellVoid=").append(pocket.getBlockState(centre.offset(33, 0, 0))
        .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VOID.get())).append("\n");
sb.append("ringDirt=").append(pocket.getBlockState(centre.offset(32, 0, 0))
        .is(net.minecraft.world.level.block.Blocks.DIRT)).append("\n");
sb.append("insideAir=").append(pocket.getBlockState(centre.offset(10, 10, 10)).isAir()).append("\n");
sb.append("padIsArcaneStone=").append(pocket.getBlockState(centre).getBlock()
        == dev.tc4port.thaumcraft.registry.TCBlocks.byId("arcane_stone").get()).append("\n");
sb.append("padAirAbove=").append(pocket.getBlockState(centre.above()).isAir()
        || pocket.getBlockState(centre.above()).is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get()))
        .append("\n");

var doorway = fr.wind_blade.isorropia.common.world.PocketPlane.exit(id);
sb.append("doorwayIsVortex=").append(pocket.getBlockState(doorway)
        .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get())).append("\n");
if (pocket.getBlockEntity(doorway) instanceof fr.wind_blade.isorropia.common.tiles.TileVortex vortex) {
    sb.append("doorwayCheat=").append(vortex.isCheat()).append("\n");
    sb.append("doorwayAspects=").append(vortex.aspectMap()).append("\n");
    var doorwayState = pocket.getBlockState(doorway);
    for (int i = 0; i < 80; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(pocket, doorway, doorwayState, vortex);
    }
    sb.append("doorwayPadSurvives=").append(pocket.getBlockState(centre)
            .is(dev.tc4port.thaumcraft.registry.TCBlocks.byId("arcane_stone").get())).append("\n");
}

var recorded = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server).plane(id);
sb.append("recorded=").append(recorded != null).append("\n");
if (recorded != null) {
    sb.append("recordedRadius=").append(recorded.radius()).append("\n");
    sb.append("recordedAspects=").append(recorded.aspects()).append("\n");
}
return sb.toString();
