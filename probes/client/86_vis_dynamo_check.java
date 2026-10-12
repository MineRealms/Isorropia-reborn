// Vis Dynamo, part 3: reads the tile back after the wand click and proves the network can draw from it. The relay
// only adopts the dynamo as its parent on its own tick, so this runs a few seconds after 85.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var dynamoPos = net.minecraft.core.BlockPos.containing(100.5, 311.0, 103.5);
var relayPos = net.minecraft.core.BlockPos.containing(100.5, 311.0, 104.5);
var be = level.getBlockEntity(dynamoPos);
sb.append("be=").append(be == null ? "null" : be.getClass().getSimpleName());
if (be instanceof fr.wind_blade.isorropia.common.tiles.TileVisDynamo dynamo) {
    sb.append(" supplying=").append(dynamo.canSupplyVis());
    sb.append(" endpoint=").append(dynamo.networkEndpoint());
    for (var channel : dev.tc4port.thaumcraft.api.aspect.VisChannel.values()) {
        sb.append(" ").append(channel).append("=").append(dynamo.offeredCentivis(channel));
    }
}
sb.append(" using=").append(player.isUsingItem());
var held = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
sb.append(" wand_aer=").append(dev.tc4port.thaumcraft.api.wand.WandApi.view(held)
        .map(v -> String.valueOf(v.visCentivis().get(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER))).orElse("-"));
sb.append(" relay_parent=").append(level.getBlockEntity(relayPos)
        instanceof dev.tc4port.thaumcraft.block.entity.VisRelayBlockEntity relay
        ? relay.parentPos().orElse(null) : "-");

int first = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(level, relayPos,
        dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 100, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
int second = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(level, relayPos,
        dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 100, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append(" drain1=").append(first).append(" drain2=").append(second);
return sb.toString();
