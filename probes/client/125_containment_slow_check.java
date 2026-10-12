// Containment focus slow upgrade, part 2: the beam slowed the pig at amplifier 0 (one rank of slow), and the pig is
// still free. The key is released and the pig removed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var done = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    done.complete(true);
});
done.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
var id = java.util.UUID.fromString(String.valueOf(System.getProperties().remove("isorropia.slowPig")));
var entity = level.getEntity(id);
if (!(entity instanceof net.minecraft.world.entity.animal.Pig pig)) {
    return "pig=" + entity;
}
var effect = pig.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
var result = "slowed=" + (effect != null) + " amplifier=" + (effect == null ? -1 : effect.getAmplifier())
        + " free=" + pig.isAlive();
pig.discard();
return result;
