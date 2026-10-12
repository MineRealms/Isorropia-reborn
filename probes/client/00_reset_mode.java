// Second half of the reset, once the respawn has been handled: creative, peaceful, empty hands.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getInventory().clearContent();
// The first hotbar slot on both sides: a probe that moved only the server's selection would leave the client using a
// different item from the one the server put in hand.
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
player.removeAllEffects();
player.setHealth(player.getMaxHealth());
return "alive=" + player.isAlive() + " mode=" + player.gameMode.getGameModeForPlayer();
