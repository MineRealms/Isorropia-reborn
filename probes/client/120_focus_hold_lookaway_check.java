// Held wand vs. focus, part 4: after looking away the dynamo no longer owns the held use on either side. The key is
// then released and the wand put down, so no fire stream outlives the test.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var dynamoPos = new net.minecraft.core.BlockPos(140, 311, 103);
var clientHolding = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> clientHolding.complete(fr.wind_blade.isorropia.common.compat.HeldWandTargets.isHolding(mc.player, dynamoPos)));
var sb = new StringBuilder("released=").append(!fr.wind_blade.isorropia.common.compat.HeldWandTargets.isHolding(player, dynamoPos))
        .append(" clientReleased=").append(!clientHolding.get(10, java.util.concurrent.TimeUnit.SECONDS));
var done = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    done.complete(true);
});
done.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
player.getInventory().clearContent();
System.getProperties().remove("isorropia.focusHold");
return sb.toString();
