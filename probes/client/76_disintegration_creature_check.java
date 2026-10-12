// Disintegration focus, part 7: the iron golem should be hurt and slowed by the beam. Releases the use key.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
sb.append("using=").append(player.isUsingItem());
var box = player.getBoundingBox().inflate(10.0);
var found = new StringBuilder();
for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
    if (mob instanceof net.minecraft.world.entity.player.Player) {
        continue;
    }
    found.append(" ").append(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))
         .append(" hp=").append(mob.getHealth()).append("/").append(mob.getMaxHealth())
         .append(" slow=").append(mob.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) != null);
}
sb.append(" mobs=").append(found.length() == 0 ? "NONE" : found.toString());
var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();
