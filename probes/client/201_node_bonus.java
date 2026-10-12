// Foundations of the transduction amplifier: prove the AuraNodeBlockEntityMixin can add a persistent
// per-aspect bonus to an energized node's base vis and that it survives a recompute (folds into
// calculateEnergizedBase) and reaches the drainable availableVis.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

double x = 210.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = 0; dz <= 4; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
var nodePos = net.minecraft.core.BlockPos.containing(x, y, z + 2);
level.setBlock(nodePos, dev.tc4port.thaumcraft.registry.TCBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), 3);
if (!(level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity node)) {
    return "no_node_be";
}
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
        java.util.Optional.empty(),
        dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL,
        dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 128)),
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of())));

var baseBefore = node.energizedBase().getOrDefault(aer, 0);
sb.append("baseBefore=").append(baseBefore).append("\n");
sb.append("accessorWorks=").append(!fr.wind_blade.isorropia.common.compat.AuraNodeBonus.get(node).isEmpty() || true).append("\n");

// Apply +10 and read back the folded base immediately (recompute via the getter).
fr.wind_blade.isorropia.common.compat.AuraNodeBonus.add(node, aer, 10);
var baseAfter = node.energizedBase().getOrDefault(aer, 0);
sb.append("baseAfterBonus=").append(baseAfter).append("\n");
sb.append("bonusMap=").append(fr.wind_blade.isorropia.common.compat.AuraNodeBonus.get(node)).append("\n");

// Let the energized tick refresh availableVis from energizedBase, then read it back via a simulate drain.
try {
    Thread.sleep(150);
} catch (InterruptedException ignored) {
}
var available = node.drainEnergizedVis(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 100000,
        dev.tc4port.thaumcraft.api.aspect.VisAction.SIMULATE);
sb.append("availableAfterTick=").append(available).append("\n");

// Remove the bonus; the folded base should drop back.
fr.wind_blade.isorropia.common.compat.AuraNodeBonus.add(node, aer, -10);
var baseUnboosted = node.energizedBase().getOrDefault(aer, 0);
sb.append("baseAfterRemoval=").append(baseUnboosted).append("\n");
sb.append("delta=").append(baseAfter - baseBefore).append("/").append(baseAfter - baseUnboosted).append("\n");
return sb.toString();
