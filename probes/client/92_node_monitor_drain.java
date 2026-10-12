// Node Monitor, part 2 (after 91): reports the monitor idle over a healthy node, then drains the node's ignis to one
// point, which is critical; 93 checks the monitor's signal a moment later.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var nodePos = new net.minecraft.core.BlockPos(140, 311, 103);
var monitorPos = nodePos.above();
var state = level.getBlockState(monitorPos);
var sb = new StringBuilder("healthy: powered=")
        .append(state.getValue(fr.wind_blade.isorropia.common.blocks.BlockNodeMonitor.POWERED))
        .append(" signal=").append(state.getSignal(level, monitorPos, net.minecraft.core.Direction.DOWN));
var node = (dev.tc4port.thaumcraft.api.node.AuraNodeView) level.getBlockEntity(nodePos);
var vis = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
vis.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:aer"), 20);
vis.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis"), 1);
var before = node.nodeState();
var result = dev.tc4port.thaumcraft.api.node.NodeApi.replaceLoadedState(level, nodePos, before,
        before.withCurrentVis(new dev.tc4port.thaumcraft.api.node.NodeVis(vis)),
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("\ndrained=").append(result.successful()).append(" vis=").append(node.nodeState().currentVis().amounts());
return sb.toString();
