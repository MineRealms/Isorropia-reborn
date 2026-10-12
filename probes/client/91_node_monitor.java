// Node Monitor, part 1: an aura node of 20 aer and 20 ignis on a pad east of the dynamos, with a node monitor on its
// top face. A healthy node gives no signal; 92 drains the ignis to one point and 93 checks the signal comes on.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();
double x = 140.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 3; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
var nodePos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 3);
var monitorPos = nodePos.above();
level.setBlockAndUpdate(nodePos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("aura_node").defaultBlockState());
if (!(level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.api.node.AuraNodeView node)) {
    return "no node block entity at " + nodePos;
}
var vis = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
vis.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:aer"), 20);
vis.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis"), 20);
var healthy = new dev.tc4port.thaumcraft.api.node.NodeVis(vis);
var before = node.nodeState();
var result = dev.tc4port.thaumcraft.api.node.NodeApi.replaceLoadedState(level, nodePos, before,
        before.withVis(healthy, healthy), dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("node=").append(nodePos.toShortString()).append(" set=").append(result.successful())
  .append(" vis=").append(node.nodeState().currentVis().amounts());
level.setBlockAndUpdate(monitorPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.NODE_MONITOR.get().defaultBlockState()
        .setValue(fr.wind_blade.isorropia.common.blocks.BlockNodeMonitor.FACING, net.minecraft.core.Direction.UP));
sb.append(" monitor=").append(level.getBlockEntity(monitorPos) instanceof fr.wind_blade.isorropia.common.tiles.TileNodeMonitor)
  .append(" survives=").append(level.getBlockState(monitorPos).canSurvive(level, monitorPos));
return sb.toString();
