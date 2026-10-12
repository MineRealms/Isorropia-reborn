// Vis Dynamo selection screen, part 3: the client's click flipped aer on the server and the data slot carried it
// back to the screen. Every switch is then turned back on and the menu closed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var dynamoPos = new net.minecraft.core.BlockPos(140, 311, 103);
var dynamo = (fr.wind_blade.isorropia.common.tiles.TileVisDynamo) level.getBlockEntity(dynamoPos);
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
var shown = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> shown.complete(mc.player.containerMenu instanceof fr.wind_blade.isorropia.common.menu.VisDynamoMenu menu
        && !menu.enabled(aer)));
var sb = new StringBuilder("aerOff=").append(!dynamo.isEnabled(aer))
        .append(" clientAerOff=").append(shown.get(10, java.util.concurrent.TimeUnit.SECONDS));
for (var channel : dev.tc4port.thaumcraft.api.aspect.VisChannel.values()) {
    if (!dynamo.isEnabled(channel)) dynamo.toggle(channel);
}
player.closeContainer();
return sb.toString();
