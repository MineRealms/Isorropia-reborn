// Node Monitor, part 3 (after 92): the critical node turns the monitor's signal on. Then the node is removed and the
// monitor must drop off it, as TH's did.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var nodePos = new net.minecraft.core.BlockPos(140, 311, 103);
var monitorPos = nodePos.above();
var state = level.getBlockState(monitorPos);
var sb = new StringBuilder("critical: powered=")
        .append(state.getValue(fr.wind_blade.isorropia.common.blocks.BlockNodeMonitor.POWERED))
        .append(" signal=").append(state.getSignal(level, monitorPos, net.minecraft.core.Direction.DOWN))
        .append(" strong=").append(state.getDirectSignal(level, monitorPos, net.minecraft.core.Direction.DOWN));
return sb.toString();
