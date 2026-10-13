// Pocket plane stage 2c: caves and pockets. A plane of earth and order is drawn by drawLayers into a solid ball, then the
// new drawCavesAndPockets pass should hollow caverns out of it and lace it with buried ore veins. Counts air strictly
// inside the sphere before and after the pass, and tallies the veins it produced.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level\n";
}

var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var ordo = dev.tc4port.thaumcraft.api.aspect.VisChannel.ORDO.aspectId();
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(terra, 40);
aspects.put(ordo, 20);

int radius = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects);
sb.append("radius=").append(radius).append("\n");
sb.append("noise=").append(fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.calcNoise(aspects)).append("\n");

var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Probe Caves", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
sb.append("id=").append(id).append("\n");

// The layered ball is solid, so every air block strictly inside it is a hole the cave pass cut.
int airBefore = 0;
for (int dx = -radius; dx <= radius; dx += 2) {
    for (int dz = -radius; dz <= radius; dz += 2) {
        for (int dy = -radius; dy <= radius; dy += 2) {
            if (dx * dx + dy * dy + dz * dz <= (radius - 3) * (radius - 3)
                    && pocket.getBlockState(new net.minecraft.core.BlockPos(centre.getX() + dx, centre.getY() + dy,
                            centre.getZ() + dz)).isAir()) {
                airBefore++;
            }
        }
    }
}
sb.append("airBefore=").append(airBefore).append("\n");

fr.wind_blade.isorropia.common.world.PocketPlaneTerrain.drawCavesAndPockets(pocket, radius, centre, aspects);

int airAfter = 0;
int infused = 0;
int crystals = 0;
java.util.Map<String, Integer> tally = new java.util.TreeMap<>();
for (int dx = -radius; dx <= radius; dx += 2) {
    for (int dz = -radius; dz <= radius; dz += 2) {
        for (int dy = -radius; dy <= radius; dy += 2) {
            if (dx * dx + dy * dy + dz * dz > (radius - 3) * (radius - 3)) {
                continue;
            }
            var state = pocket.getBlockState(new net.minecraft.core.BlockPos(centre.getX() + dx, centre.getY() + dy,
                    centre.getZ() + dz));
            if (state.isAir()) {
                airAfter++;
                continue;
            }
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            tally.merge(name, 1, Integer::sum);
            if (name.contains("infused_stone") || name.equals("cinnabar_ore") || name.equals("amber_bearing_stone")) {
                infused++;
            }
            if (name.contains("crystal_cluster")) {
                crystals++;
            }
        }
    }
}
sb.append("airAfter=").append(airAfter).append("\n");
sb.append("airAdded=").append(airAfter - airBefore).append("\n");
sb.append("veinBlocks=").append(infused).append("\n");
sb.append("crystalBlocks=").append(crystals).append("\n");
sb.append("tally=").append(tally).append("\n");
sb.append("cavesOpenedAir=").append(airAfter > airBefore).append("\n");
sb.append("pocketVeinsFound=").append(infused > 0).append("\n");
return sb.toString();
