// Recombinator: prove registration (block/item/be/research/recipe) and that a redstone-powered machine above a node
// fuses a primal from a second nearby node into their compound aspect, adding it to the node below and consuming both
// primals. Uses ordo (receiver) + ignis (donor), which TC4R knows as potentia.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

// --- registration checks ---
var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
        fr.wind_blade.isorropia.common.items.ItemsIS.RECOMBINATOR.get());
sb.append("itemId=").append(itemId).append("\n");
sb.append("blockPresent=").append(fr.wind_blade.isorropia.common.blocks.BlocksIS.RECOMBINATOR.isPresent()).append("\n");
sb.append("bePresent=").append(fr.wind_blade.isorropia.common.tiles.TilesIS.RECOMBINATOR.isPresent()).append("\n");

var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/recombinator");
var recipe = server.getRecipeManager().byKey(rl);
sb.append("recipePresent=").append(recipe.isPresent()).append("\n");
if (recipe.isPresent()) {
    sb.append("recipeType=").append(recipe.get().getType()).append("\n");
}

var research = dev.tc4port.thaumcraft.research.ResearchCatalog.get(
        new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", "recombinator"));
sb.append("researchPresent=").append(research != null).append("\n");
if (research != null) {
    sb.append("parents=").append(research.parents()).append("\n");
    sb.append("pages=").append(research.pages().size()).append("\n");
}

// --- runtime: fuse ordo + ignis on two ordinary nodes ---
var receiverPos = new net.minecraft.core.BlockPos(250, 312, 150);
var machinePos = receiverPos.above();
var donorPos = machinePos.offset(2, -1, 0);
for (int dx = -1; dx <= 3; dx++) {
    for (int dz = -2; dz <= 2; dz++) {
        level.setBlock(receiverPos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}

var ordo = dev.tc4port.thaumcraft.api.aspect.VisChannel.ORDO.aspectId();
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS.aspectId();
level.setBlock(receiverPos, dev.tc4port.thaumcraft.registry.TCBlocks.AURA_NODE.get().defaultBlockState(), 3);
level.setBlock(donorPos, dev.tc4port.thaumcraft.registry.TCBlocks.AURA_NODE.get().defaultBlockState(), 3);
if (!(level.getBlockEntity(receiverPos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity receiver)
        || !(level.getBlockEntity(donorPos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity donor)) {
    return sb.append("no_node_be\n").toString();
}
receiver.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
        java.util.Optional.empty(), dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL,
        dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ordo, 8)),
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ordo, 8))));
donor.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
        java.util.Optional.empty(), dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL,
        dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ignis, 8)),
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ignis, 8))));

var fused = dev.tc4port.thaumcraft.api.aspect.AspectApi.combination(ordo, ignis);
sb.append("fused=").append(fused).append("\n");

// Diagnostics: the two-node transaction only commits on the server thread.
sb.append("serverThread=").append(server.isSameThread()).append("\n");
var sameThread = dev.tc4port.thaumcraft.api.node.NodeApi.replaceLoadedStates(level, receiverPos,
        receiver.nodeState(), receiver.nodeState().withType(dev.tc4port.thaumcraft.api.node.NodeTypeId.HUNGRY),
        donorPos, donor.nodeState(), donor.nodeState().withType(dev.tc4port.thaumcraft.api.node.NodeTypeId.HUNGRY),
        dev.tc4port.thaumcraft.api.aspect.VisAction.SIMULATE);
sb.append("replaceProbe=").append(sameThread).append("\n");

level.setBlock(machinePos, fr.wind_blade.isorropia.common.blocks.BlocksIS.RECOMBINATOR.get().defaultBlockState(), 3);
level.setBlock(machinePos.north(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
if (!(level.getBlockEntity(machinePos) instanceof fr.wind_blade.isorropia.common.tiles.TileRecombinator machine)) {
    return sb.append("no_machine_be\n").toString();
}
machine.setShouldActivate(true);
var machineState = level.getBlockState(machinePos);
var before = receiver.nodeState().baseVis().amounts();

int ticks = 0;
for (; ticks < 20000 && receiver.nodeState().baseVis().amounts().equals(before); ticks++) {
    fr.wind_blade.isorropia.common.tiles.TileRecombinator.serverTick(level, machinePos, machineState, machine);
}
sb.append("ticksToFuse=").append(ticks).append("\n");
sb.append("activated=").append(machine.isActivated()).append("\n");
sb.append("count=").append(machine.count()).append("\n");
sb.append("receiverBaseAfter=").append(receiver.nodeState().baseVis().amounts()).append("\n");
sb.append("receiverCurrentAfter=").append(receiver.nodeState().currentVis().amounts()).append("\n");
sb.append("donorCurrentAfter=").append(donor.nodeState().currentVis().amounts()).append("\n");
sb.append("gainedFused=").append(fused.map(f -> receiver.nodeState().baseVis().amount(f) > 0).orElse(false)).append("\n");
sb.append("receiverOrdoEroded=").append(receiver.nodeState().baseVis().amount(ordo) == 7).append("\n");
sb.append("receiverCurrentMatchesBase=").append(
        receiver.nodeState().currentVis().amounts().equals(receiver.nodeState().baseVis().amounts())).append("\n");
sb.append("donorIgnisEroded=").append(donor.nodeState().currentVis().amount(ignis) == 7).append("\n");

// clean up
level.removeBlock(machinePos.north(), false);
level.removeBlock(machinePos, false);
level.removeBlock(donorPos, false);
level.removeBlock(receiverPos, false);
return sb.toString();
