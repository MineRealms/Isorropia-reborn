// Vis Dynamo, part 5: after the release the supply window has run out, so the dynamo is no longer a network
// source and the relay can draw nothing from it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var dynamoPos = net.minecraft.core.BlockPos.containing(100.5, 311.0, 103.5);
var relayPos = net.minecraft.core.BlockPos.containing(100.5, 311.0, 104.5);
if (level.getBlockEntity(dynamoPos) instanceof fr.wind_blade.isorropia.common.tiles.TileVisDynamo dynamo) {
    sb.append("supplying=").append(dynamo.canSupplyVis());
    sb.append(" AER=").append(dynamo.offeredCentivis(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER));
}
sb.append(" using=").append(server.getPlayerList().getPlayer(mc.player.getUUID()).isUsingItem());
sb.append(" drain=").append(dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(level, relayPos,
        dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 100, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE));
return sb.toString();
