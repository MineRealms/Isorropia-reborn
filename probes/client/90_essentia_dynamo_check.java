// Essentia Dynamo, part 2 (run a few seconds after 89): the dynamo has been burning the jar's ignis, offers twelve
// centivis of ignis a tick, and the relay beside it can draw them once, then nothing more that tick.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var jarPos = new net.minecraft.core.BlockPos(120, 310, 103);
var dynamoPos = jarPos.above();
var relayPos = dynamoPos.east();
var sb = new StringBuilder();
if (!(level.getBlockEntity(dynamoPos) instanceof fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo dynamo)) {
    return "no dynamo at " + dynamoPos;
}
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS;
// Between two units the dynamo has one tick with nothing left (TH's had it too); step over it so the drain is tested.
if (dynamo.ticksProvided() == 0) {
    fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo.serverTick(level, dynamoPos, level.getBlockState(dynamoPos), dynamo);
    sb.append("(stepped over the gap between units) ");
}
sb.append("ticks=").append(dynamo.ticksProvided())
  .append(" offered ignis=").append(dynamo.providedCentivis(ignis))
  .append(" aer=").append(dynamo.providedCentivis(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER))
  .append(" supplying=").append(dynamo.canSupplyVis())
  .append(" endpoint=").append(dynamo.networkEndpoint() == null ? "none" : dynamo.networkEndpoint().toShortString());
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    sb.append(" jarLeft=").append(jar.essentiaAmount(net.minecraft.core.Direction.UP));
}
int first = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drainRelay(level, relayPos, ignis, 50,
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
int second = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drainRelay(level, relayPos, ignis, 50,
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("\ndrain1=").append(first).append(" drain2=").append(second);
return sb.toString();
