// Start holding against an empty shard; nothing can refill it until 106. No repeated clicks after this setup.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var pos = new net.minecraft.core.BlockPos(200, 311, 103);
var block = fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get();
level.removeBlock(pos, false);
level.setBlock(pos, block.defaultBlockState(), 3);
var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
shard.addEssence(dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
        dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(aer, 2)));
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.create(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD,
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.stopUsingItem();
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
player.connection.teleport(200.5, 310, 100.5, 0F, 0F);
level.getChunkSource().move(player);
var clientWand = wand.copy();
var ready = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0F);
    mc.player.setXRot(0F);
    var key = mc.options.keyUse.getKey();
    net.minecraft.client.KeyMapping.set(key, false);
    net.minecraft.client.KeyMapping.set(key, true);
    net.minecraft.client.KeyMapping.click(key);
    ready.complete(true);
});
ready.get(10, java.util.concurrent.TimeUnit.SECONDS);
return "empty=" + (shard.stored(aer) == 0);
