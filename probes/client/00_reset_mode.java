// Second half of the reset, once the respawn has been handled: creative, peaceful, empty hands.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getInventory().clearContent();
player.removeAllEffects();
player.setHealth(player.getMaxHealth());
return "alive=" + player.isAlive() + " mode=" + player.gameMode.getGameModeForPlayer();
