// Puts the player back in a known state before a sweep: alive, in creative, peaceful, with nothing in hand.
// A survival death in an earlier run would otherwise leave every later probe working on a dead player.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var revived = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    if (mc.player != null && !mc.player.isAlive()) {
        mc.player.respawn();
        revived.complete("respawned");
    } else {
        revived.complete("alive");
    }
    mc.setScreen(null);
});
var state = revived.get(5, java.util.concurrent.TimeUnit.SECONDS);
// The client's respawn request only reaches a player the server still tracks. One an earlier probe removed
// (a vat that swallowed it, a beacon that moved it) has to be brought back on the server directly, or the
// client's request is silently dropped and the player stays gone for the whole sweep.
var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
if (sp != null && sp.isRemoved()) {
    server.getPlayerList().respawn(sp, false);
    state += " server-respawned";
}
return state + " (the server applies it once this probe returns)";
