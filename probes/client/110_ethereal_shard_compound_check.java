var level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().overworld();
var victus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("victus");
var fullPos = new net.minecraft.core.BlockPos(230, 311, 103);
var partialPos = new net.minecraft.core.BlockPos(254, 311, 103);
var full = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(fullPos);
var partial = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(partialPos);
boolean complete = full.stored(victus) > 0 && full.stored(victus) <= full.capacity(victus);
boolean missing = partial.stored(victus) == 0;
String result = "compoundCharged=" + complete + " fullStored=" + full.stored(victus)
        + " missingChannelBlocked=" + missing + " partialStored=" + partial.stored(victus);
for (var pos : java.util.List.of(fullPos, partialPos)) {
    level.removeBlock(pos.south(), false);
    level.removeBlock(pos.south(2), false);
}
return result;
