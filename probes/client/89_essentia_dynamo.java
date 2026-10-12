// Essentia Dynamo, part 1: a warded jar of 32 ignis with an essentia dynamo on top and a vis relay beside it, on a
// pad east of the vis dynamo's. The dynamo draws from the jar and feeds the relay; check with 90 a few seconds later.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
double x = 120.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 3; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
var jarPos = net.minecraft.core.BlockPos.containing(x, y, z + 3);
var dynamoPos = jarPos.above();
var relayPos = dynamoPos.east();
level.setBlockAndUpdate(jarPos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar").defaultBlockState());
long filled = 0;
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    filled = jar.addEssentia(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis"), 32,
            net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
level.setBlockAndUpdate(dynamoPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.ESSENTIA_DYNAMO.get().defaultBlockState());
level.setBlockAndUpdate(relayPos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("vis_relay").defaultBlockState());
sb.append("jar=").append(jarPos.toShortString()).append(" filled=").append(filled)
  .append(" dynamo=").append(level.getBlockEntity(dynamoPos) instanceof fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo)
  .append(" relay=").append(relayPos.toShortString());
// The primals a compound is split between, as TH's reduceToPrimals.
sb.append("\nprimals ignis=").append(fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo.primalsOf(
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis")))
  .append(" victus=").append(fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo.primalsOf(
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:victus")))
  .append(" humanus=").append(fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo.primalsOf(
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:humanus")));
return sb.toString();
