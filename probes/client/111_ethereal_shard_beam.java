// Long-lived wand draw for screenshots; execute after the functional sweep, then guishot while the key stays held.
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
var essence = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
        dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(aer, 2));
essence.setCount(64);
shard.addEssence(essence);
shard.addToStore(aer, 256);
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.create(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.parse("silverwood"),
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.parse("thaumium"));
player.stopUsingItem();
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
player.connection.teleport(200.5, 310, 100.5, 0F, 0F);
level.getChunkSource().move(player);
var clientWand = wand.copy();
mc.execute(() -> {
    mc.setScreen(null);
    mc.options.hideGui = false;
    mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0F);
    mc.player.setXRot(0F);
    var key = mc.options.keyUse.getKey();
    net.minecraft.client.KeyMapping.set(key, false);
    net.minecraft.client.KeyMapping.set(key, true);
    net.minecraft.client.KeyMapping.click(key);
});
return "beamReady=true capacity=" + shard.capacity(aer);
