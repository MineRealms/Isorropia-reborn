// Held wand vs. focus, part 1 (control): a fully charged wand with Thaumcraft's fire focus, held at the open sky.
// The fire stream must cost ignis here, or the machine half of the test (118 -> 119) would prove nothing.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var released = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    released.complete(true);
});
released.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
player.closeContainer();
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
var installed = dev.tc4port.thaumcraft.api.focus.FocusApi.install(player, net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("focus_fire")),
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
var vis = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow().visCentivis();
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS;
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
System.getProperties().put("isorropia.focusHold", vis.get(aer) + "," + vis.get(ignis));
player.connection.teleport(140.5, 310.0, 100.5, 0.0F, -80.0F);
level.getChunkSource().move(player);
var clientWand = player.getMainHandItem().copy();
var pressed = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(-80.0F);
    var key = mc.options.keyUse.getKey();
    net.minecraft.client.KeyMapping.set(key, true);
    net.minecraft.client.KeyMapping.click(key);
    pressed.complete(true);
});
pressed.get(10, java.util.concurrent.TimeUnit.SECONDS);
return "install=" + installed + " hooked=" + fr.wind_blade.isorropia.common.compat.HeldWandTargets.hooked();
