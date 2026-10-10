// Spawns every Isorropia creature in a row on a stone strip, frozen and facing the camera, for screenshots.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var base = new net.minecraft.core.BlockPos(48, 299, 50);
String[] ids = {"isorropia:taint_pig", "isorropia:gravekeeper", "isorropia:sheeder", "isorropia:chromatic_sheep",
        "isorropia:scholar_chicken", "isorropia:ore_pig", "isorropia:jelly_rabbit", "isorropia:chocolate_cow",
        "isorropia:golden_chicken", "isorropia:guardian_panther", "isorropia:nether_hound"};
// Clear old probe creatures.
var box = new net.minecraft.world.phys.AABB(base).inflate(40, 6, 6);
for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) {
    old.discard();
}
for (int dx = -2; dx < ids.length * 3 + 2; dx++) {
    for (int dz = -2; dz <= 2; dz++) {
        level.setBlockAndUpdate(base.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
    }
}
var sb = new StringBuilder();
for (int i = 0; i < ids.length; i++) {
    var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse(ids[i]));
    var entity = type.create(level);
    if (entity instanceof net.minecraft.world.entity.Mob mob) {
        mob.moveTo(base.getX() + i * 3 + 0.5, base.getY(), base.getZ() + 0.5, 180.0F, 0.0F);
        mob.setYHeadRot(180.0F);
        mob.setYBodyRot(180.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        sb.append(ids[i].substring(10)).append("@x=").append(base.getX() + i * 3).append(" ");
    }
}
return sb.toString();
