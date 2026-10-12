// Ethereal Shard, part 2: proves the shard has been banking vis out of the network, then tears that source down so
// the wand draw below is not racing the refill. The wizard then right-clicks the shard with the wand, which runs
// the whole path: WandItem.onItemUseFirst -> BlockEtherealShard.use -> TileEtherealShard.beginDraining. Probe 103
// reads back what the shard did.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var shardPos = net.minecraft.core.BlockPos.containing(200.5, 311.0, 103.5);
var relayPos = net.minecraft.core.BlockPos.containing(200.5, 311.0, 104.5);
var nodePos = net.minecraft.core.BlockPos.containing(200.5, 311.0, 105.5);
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();

if (!(level.getBlockEntity(shardPos) instanceof fr.wind_blade.isorropia.common.tiles.TileEtherealShard shard)) {
    return "shard=missing";
}
// Five vis went in by hand in 101; anything above that came out of the network on the shard's own ticker.
sb.append("stored=").append(shard.stored(aer));
sb.append(" refilled=").append(shard.stored(aer) > 5);
int drained = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(level, shardPos,
        dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 10,
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append(" network=").append(drained > 0);
var synced = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> synced.complete(mc.level.getBlockEntity(shardPos)
        instanceof fr.wind_blade.isorropia.common.tiles.TileEtherealShard clientShard
        && clientShard.stored(aer) > 5));
sb.append(" clientRefilled=").append(synced.get(10, java.util.concurrent.TimeUnit.SECONDS));
level.setBlock(relayPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
level.setBlock(nodePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

// The client's own aim is what the shard reads back each tick, so the click is driven through the client.
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var clientWand = player.getMainHandItem().copy();
var use = mc.options.keyUse.getKey();
var clicked = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.stopUsingItem();
    mc.player.getInventory().selected = 0;
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    net.minecraft.client.KeyMapping.set(use, false);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
    clicked.complete(true);
});
clicked.get(10, java.util.concurrent.TimeUnit.SECONDS);
return sb.toString();
