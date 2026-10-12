// Node Monitor, part 4 (after 93): removes the node from under the monitor; 95 checks the monitor fell off as an item.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var nodePos = new net.minecraft.core.BlockPos(140, 311, 103);
level.setBlockAndUpdate(nodePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
return "node removed";
