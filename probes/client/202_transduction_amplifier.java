// Transduction amplifier: prove it is registered (block/item/research/recipe) and that, against a valid
// energized node stack (stabilizer / node / transducer), flipping shouldActivate drives a +10-per-aspect
// boost through AuraNodeBonus and back to normal.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

// --- registration checks (mirror probe 200) ---
var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
        fr.wind_blade.isorropia.common.items.ItemsIS.TRANSDUCTION_AMPLIFIER.get());
sb.append("itemId=").append(itemId).append("\n");
sb.append("blockPresent=").append(fr.wind_blade.isorropia.common.blocks.BlocksIS.TRANSDUCTION_AMPLIFIER.isPresent()).append("\n");

var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "arcane/transduction_amplifier");
var recipe = server.getRecipeManager().byKey(rl);
sb.append("recipePresent=").append(recipe.isPresent()).append("\n");
if (recipe.isPresent()) {
    var r = recipe.get();
    sb.append("recipeType=").append(r.getType()).append("\n");
    var resultId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.getResultItem(server.registryAccess()).getItem());
    sb.append("recipeResult=").append(resultId).append("\n");
}

var research = dev.tc4port.thaumcraft.research.ResearchCatalog.get(
        new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", "transductionAmplifier"));
sb.append("researchPresent=").append(research != null).append("\n");
if (research != null) {
    sb.append("parents=").append(research.parents()).append("\n");
    sb.append("pages=").append(research.pages().size()).append("\n");
}

// --- runtime boost/unboost against a valid energized node stack ---
var nodePos = new net.minecraft.core.BlockPos(210, 311, 120);
var stabPos = nodePos.below();
var transPos = nodePos.above();
// Solid floor under the stabilizer.
for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
    level.setBlock(stabPos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
level.setBlock(stabPos, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_STABILIZER.get().defaultBlockState(), 3);
level.setBlock(transPos, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_TRANSDUCER.get().defaultBlockState(), 3);
level.setBlock(nodePos, dev.tc4port.thaumcraft.registry.TCBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), 3);
sb.append("stabBlock=").append(level.getBlockState(stabPos).getBlock()).append("\n");
sb.append("nodeBlock=").append(level.getBlockState(nodePos).getBlock()).append("\n");
sb.append("transBlock=").append(level.getBlockState(transPos).getBlock()).append("\n");
if (!(level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity node)) {
    return sb.append("no_node_be\n").toString();
}
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
        java.util.Optional.empty(),
        dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL,
        dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 128)),
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of())));
var baseBefore = node.energizedBase().getOrDefault(aer, 0);

// Place the amplifier on the north face of the node (the node is +Z / south of the amp).
var facing = net.minecraft.core.Direction.SOUTH;
var ampPos = nodePos.relative(facing.getOpposite());
var ampState = fr.wind_blade.isorropia.common.blocks.BlocksIS.TRANSDUCTION_AMPLIFIER.get()
        .defaultBlockState().setValue(fr.wind_blade.isorropia.common.blocks.BlockTransductionAmplifier.FACING, facing);
level.setBlock(ampPos, ampState, 3);
var tile = level.getBlockEntity(ampPos);
if (!(tile instanceof fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier amp)) {
    return sb.append("\ntileNotAmplifier\n").toString();
}
sb.append("nodeStillThere=").append(level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity).append("\n");

// First tick initialises fireOnce (reads no redstone signal -> shouldActivate false); after it the amplifier
// has resolved its facing and can find its node.
fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos, ampState, amp);
sb.append("nodeFound=").append(amp.nodeFor(level, ampPos) != null).append("\n");
// Energise it.
amp.setShouldActivate(true);
fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos, ampState, amp);
var baseBoosted = node.energizedBase().getOrDefault(aer, 0);
sb.append("baseBefore=").append(baseBefore).append("\n");
sb.append("baseBoosted=").append(baseBoosted).append("\n");
sb.append("activated=").append(amp.isActivated()).append("\n");

// Switch it off again; the bonus should unwind.
amp.setShouldActivate(false);
fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos, ampState, amp);
var baseAfter = node.energizedBase().getOrDefault(aer, 0);
sb.append("baseAfterUnboost=").append(baseAfter).append("\n");
sb.append("delta=").append(baseBoosted - baseBefore).append("/").append(baseAfter - baseBefore).append("\n");
sb.append("deactivated=").append(!amp.isActivated()).append("\n");

// clean up
level.removeBlock(ampPos, false);
level.removeBlock(transPos, false);
level.removeBlock(nodePos, false);
level.removeBlock(stabPos, false);
return sb.toString();
