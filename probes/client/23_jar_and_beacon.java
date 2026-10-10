// Captures a creature into a soul jar, places it, and puts a soul beacon beside it, for a screenshot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var base = new net.minecraft.core.BlockPos(50, 300, 50);
level.setChunkForced(base.getX() >> 4, base.getZ() >> 4, true);
// A clean stage: stone floor, sky above.
for (int dx = -3; dx <= 4; dx++) {
    for (int dz = -3; dz <= 3; dz++) {
        level.setBlockAndUpdate(base.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
        for (int dy = 0; dy <= 4; dy++) {
            level.setBlockAndUpdate(base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}
// Leftovers from other probes would be in frame.
for (var stray : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
        new net.minecraft.world.phys.AABB(base).inflate(24, 6, 24))) {
    stray.discard();
}
var sb = new StringBuilder();
// A cow, jarred through the same call the portability infusion uses.
var cow = net.minecraft.world.entity.EntityType.COW.create(level);
cow.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
level.addFreshEntity(cow);
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
// The capture spends a warded jar, so give the probe one.
player.getInventory().add(new net.minecraft.world.item.ItemStack(
        dev.tc4port.thaumcraft.api.ThaumcraftContent.item("warded_jar")));
boolean captured = fr.wind_blade.isorropia.common.infusion.SoulJar.capture(player, cow);
sb.append("captured=").append(captured);
// Place one of those jars in the world next to the beacon.
var jarItem = player.getInventory().items.stream()
        .filter(stack -> stack.is(fr.wind_blade.isorropia.common.items.ItemsIS.JAR_SOUL.get())).findFirst().orElse(null);
sb.append(" jarInInventory=").append(jarItem != null);
var jarPos = base.offset(2, 0, 0);
level.setBlockAndUpdate(jarPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.JAR_SOUL.get().defaultBlockState()
        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
if (jarItem != null && level.getBlockEntity(jarPos) instanceof fr.wind_blade.isorropia.common.tiles.TileJarSoul jar) {
    var data = jarItem.getTag() == null ? null : jarItem.getTag().getCompound(fr.wind_blade.isorropia.common.infusion.SoulJar.ENTITY_TAG);
    jar.setEntityData(data);
    sb.append(" jarData=").append(data == null ? "none" : data.getString("id"));
}
// The soul beacon, bound to the player.
var beaconPos = base;
level.setBlockAndUpdate(beaconPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.SOUL_BEACON.get().defaultBlockState());
if (level.getBlockEntity(beaconPos) instanceof fr.wind_blade.isorropia.common.tiles.TileSoulBeacon beacon) {
    sb.append(" activated=").append(beacon.activate(player));
}
return sb.toString();
