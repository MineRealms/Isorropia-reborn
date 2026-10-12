// Vis Dynamo, part 4: the wizard lets go of the wand (key released first, or the client's held-use branch keeps
// re-clicking). The supply window is ten ticks, so the read-back is in 88.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var use = mc.options.keyUse.getKey();
mc.execute(() -> {
    net.minecraft.client.KeyMapping.set(use, false);
    mc.options.keyUse.setDown(false);
});
player.stopUsingItem();
return "using=" + player.isUsingItem();
