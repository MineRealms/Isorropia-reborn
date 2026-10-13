// Integration probe for the three parallel modules: one plane carrying earth, life, taint and motion should come out
// with the layered terrain, the caves and pockets, the tainted biome written over its chunks, the plane's effect list
// filled, and the landing pad and exit rift carved through all of it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level\n";
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

var biome = fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(aspects);
sb.append("chosenBiome=").append(biome.location()).append("\n");
sb.append("isCold=").append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(aspects)).append("\n");
sb.append("noise=").append(fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.calcNoise(aspects)).append("\n");
sb.append("life=").append(fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.calcLife(aspects)).append("\n");

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Integration Plane", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
var radius = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects);
sb.append("id=").append(id).append(" radius=").append(radius).append("\n");

// The biome the chooser picked must be what the stamped chunks actually report.
var reported = pocket.getBiome(centre);
sb.append("reportedBiome=").append(reported.unwrapKey().map(k -> k.location().toString()).orElse("?"))
        .append("\n");
sb.append("biomeMatches=").append(reported.is(biome)).append("\n");

// Tally what the terrain passes left behind across a slab of the plane.
java.util.Map<String, Integer> tally = new java.util.TreeMap<>();
int airInside = 0;
for (int dx = -30; dx <= 30; dx += 2) {
    for (int dz = -30; dz <= 30; dz += 2) {
        for (int dy = -20; dy <= 20; dy += 2) {
            var pos = new net.minecraft.core.BlockPos(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
            var state = pocket.getBlockState(pos);
            if (state.isAir()) {
                airInside++;
            } else {
                String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                tally.merge(name, 1, Integer::sum);
            }
        }
    }
}
sb.append("airInside=").append(airInside).append("\n");
sb.append("tally=").append(tally).append("\n");
sb.append("hasLayers=").append(tally.containsKey("dirt") || tally.containsKey("grass_block")
        || tally.containsKey("stone")).append("\n");

// The pad and the doorway must survive every pass.
sb.append("padIntact=").append(pocket.getBlockState(centre).getBlock()
        == dev.tc4port.thaumcraft.registry.TCBlocks.byId("arcane_stone").get()).append("\n");
sb.append("doorwayIsVortex=").append(pocket.getBlockState(fr.wind_blade.isorropia.common.world.PocketPlane.exit(id))
        .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get())).append("\n");

// The plane's effect list, which TH filled and never read back.
var recorded = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server).plane(id);
sb.append("recordedEffects=").append(recorded == null ? "null" : recorded.effects()).append("\n");
return sb.toString();
