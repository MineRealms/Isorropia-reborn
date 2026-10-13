// Pocket plane layers: a plane made of earth, light and plant aspects should come out as a grass-covered dirt sphere,
// wrapped in the void bubble, with the landing pad and doorway carved through it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level\n";
}

var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var lux = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:lux");
var herba = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:herba");
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(terra, 30);
aspects.put(lux, 15);
aspects.put(herba, 15);

sb.append("noise=").append(fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.calcNoise(aspects)).append("\n");
sb.append("life=").append(fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.calcLife(aspects)).append("\n");

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Probe Layers", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
sb.append("id=").append(id).append(" radius=").append(fr.wind_blade.isorropia.common.world.PocketPlaneBuilder
        .radiusFor(aspects)).append("\n");

// Walk a column beside the landing pad to find the surface.
int scanX = centre.getX() + 12;
int scanZ = centre.getZ() + 12;
int surfaceY = Integer.MIN_VALUE;
net.minecraft.world.level.block.Block surfaceBlock = null;
net.minecraft.world.level.block.Block underBlock = null;
for (int y = centre.getY() + 60; y > centre.getY() - 60; y--) {
    var state = pocket.getBlockState(new net.minecraft.core.BlockPos(scanX, y, scanZ));
    if (!state.isAir()) {
        surfaceY = y;
        surfaceBlock = state.getBlock();
        underBlock = pocket.getBlockState(new net.minecraft.core.BlockPos(scanX, y - 1, scanZ)).getBlock();
        break;
    }
}
sb.append("surfaceY=").append(surfaceY).append("\n");
sb.append("surfaceBlock=").append(surfaceBlock).append("\n");
sb.append("underBlock=").append(underBlock).append("\n");
sb.append("airAboveSurface=").append(
        pocket.getBlockState(new net.minecraft.core.BlockPos(scanX, surfaceY + 1, scanZ)).isAir()).append("\n");

// Tally what the layers actually laid down across a slab of the plane.
java.util.Map<String, Integer> tally = new java.util.TreeMap<>();
for (int dx = -20; dx <= 20; dx += 2) {
    for (int dz = -20; dz <= 20; dz += 2) {
        for (int dy = -40; dy <= 40; dy += 2) {
            var state = pocket.getBlockState(new net.minecraft.core.BlockPos(centre.getX() + dx, centre.getY() + dy,
                    centre.getZ() + dz));
            if (!state.isAir()) {
                String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                tally.merge(name, 1, Integer::sum);
            }
        }
    }
}
sb.append("tally=").append(tally).append("\n");

// The void bubble should still wrap everything at the plane's radius.
sb.append("shellAtRadius=").append(pocket.getBlockState(centre.offset(0, 45, 0))
        .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VOID.get())).append("\n");
sb.append("padIntact=").append(pocket.getBlockState(centre).getBlock()
        == dev.tc4port.thaumcraft.registry.TCBlocks.byId("arcane_stone").get()).append("\n");
sb.append("doorwayIsVortex=").append(pocket.getBlockState(fr.wind_blade.isorropia.common.world.PocketPlane.exit(id))
        .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get())).append("\n");
return sb.toString();
