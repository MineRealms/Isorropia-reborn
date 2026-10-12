var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var pos = new net.minecraft.core.BlockPos(200, 311, 103);
var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
var future = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    if (!(mc.level.getBlockEntity(pos) instanceof fr.wind_blade.isorropia.common.tiles.TileEtherealShard clientShard)) {
        future.complete("clientShard=missing");
        return;
    }
    future.complete("clientDraining=" + clientShard.draining(mc.level) + " clientColour="
            + String.format("%06X", clientShard.drainColour()) + " clientUsing=" + mc.player.isUsingItem());
});
return "serverDraining=" + shard.draining(level) + " " + future.get(10, java.util.concurrent.TimeUnit.SECONDS);
