// S4 compat test: the kubejs server script (run-client/kubejs/server_scripts/horizons_test.js)
// adds kubejs:probe_infusion (creature) and kubejs:probe_self_infusion (self) through the
// ThaumicHorizons binding. Assert they reached the server maps and the client snapshot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
if (server == null) return "NO_SERVER";

var creatureId = new net.minecraft.resources.ResourceLocation("kubejs", "probe_infusion");
var selfId = new net.minecraft.resources.ResourceLocation("kubejs", "probe_self_infusion");
var creature = fr.wind_blade.isorropia.common.infusion.InfusionRecipes.creatureRecipe(creatureId);
var self = fr.wind_blade.isorropia.common.infusion.InfusionRecipes.selfRecipe(selfId);

var client = fr.wind_blade.isorropia.common.infusion.InfusionDisplayData.entries();
var clientIds = new java.util.HashSet();
for (var entry : client) clientIds.add(entry.id());

var serverSnapshot = fr.wind_blade.isorropia.common.infusion.InfusionDisplayData.serverEntries();
var snapshotIds = new java.util.HashSet();
for (var entry : serverSnapshot) snapshotIds.add(entry.id());

return "creature=" + creature.isPresent() + " self=" + self.isPresent()
        + " clientHas=" + clientIds.contains(creatureId) + "," + clientIds.contains(selfId)
        + " snapshotHas=" + snapshotIds.contains(creatureId) + "," + snapshotIds.contains(selfId)
        + " creatureCount=" + fr.wind_blade.isorropia.common.infusion.InfusionRecipes.creatureRecipes().size()
        + " clientCount=" + client.size();
