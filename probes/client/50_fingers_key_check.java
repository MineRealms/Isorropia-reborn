// Run after 49_keys: the morphic fingers key should have opened the portable workbench on both sides.
var mc = net.minecraft.client.Minecraft.getInstance();
var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
return "client screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName())
        + " server menu=" + player.containerMenu.getClass().getSimpleName();
