// Containment focus, the rules: bosses are immune, ordinary mobs are not, and a survival player with no empty
// jar gets no beam. Holds right-click with no jar; run 53_containment_check afterwards (the pig must remain and
// the player must have stopped using the wand).
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var wither = net.minecraft.world.entity.EntityType.WITHER.create(level);
var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
sb.append("canContain wither=").append(fr.wind_blade.isorropia.common.focus.ContainmentFocus.canContain(wither))
  .append(" ironGolem=").append(fr.wind_blade.isorropia.common.focus.ContainmentFocus.canContain(golem));

// A survival player on an open platform at night is soon knocked off it by a zombie; keep the world peaceful.
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
var wand = player.getMainHandItem();
player.getInventory().clearContent();
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
double x = 40.5, y = 310.0, z = 40.5;
player.teleportTo(level, x, y, z, 0.0F, 10.0F);
// The focus probes share this platform and leave their target blocks in the beam's path; clear it.
for (int dz = 1; dz <= 8; dz++) for (int dy = 0; dy <= 2; dy++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x, y + dy, z + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
// Earlier runs leave creatures here (pigs, a wandering cow); the beam would hold the wrong one, or hop between
// overlapping ones so no hold ever fills.
for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
        new net.minecraft.world.phys.AABB(x - 3, y - 1, z - 2, x + 3, y + 4, z + 9))) {
    stale.discard();
}
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(x, y, z + 5.0, 180.0F, 0.0F);
pig.setNoAi(true);
pig.setHealth(1.0F);
level.addFreshEntity(pig);
sb.append(" emptyJar=").append(fr.wind_blade.isorropia.common.infusion.SoulJar.hasEmptyJar(player))
  .append(" wand=").append(dev.tc4port.thaumcraft.api.wand.WandApi.view(wand).isPresent());
var use = mc.options.keyUse.getKey();
// The server only syncs the wand it just put in hand at the end of its tick, after this probe returns, so the
// client is handed the same wand directly; otherwise it would click with whatever it held before.
var clientWand = player.getMainHandItem().copy();
mc.execute(() -> {
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.setScreen(null);
    mc.player.setYRot(0.0F); mc.player.setXRot(10.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();
