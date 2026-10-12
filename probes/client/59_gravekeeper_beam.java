// The gravekeeper's holy beam: a gravekeeper and a still zombie three blocks apart on the stone platform, so the
// beam can be shot with
//   python tools/client_probe.py shot gravekeeper_beam 37.5 311.0 46.0 -90 20 1.5
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
// Undead do not exist in peaceful.
server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
var box = new net.minecraft.world.phys.AABB(34, 305, 36, 47, 316, 52);
for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) {
    old.discard();
}
var keeper = fr.wind_blade.isorropia.common.entities.EntitiesIS.GRAVEKEEPER.get().create(level);
keeper.moveTo(40.5, 310.0, 44.5, 90.0F, 0.0F);
keeper.setNoAi(true);
level.addFreshEntity(keeper);
var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
zombie.moveTo(40.5, 310.0, 47.5, 180.0F, 0.0F);
zombie.setNoAi(true);
zombie.setInvulnerable(true);
level.addFreshEntity(zombie);
return "gravekeeper=" + keeper.getId() + " zombie=" + zombie.getId() + " distance="
        + keeper.distanceTo(zombie);
