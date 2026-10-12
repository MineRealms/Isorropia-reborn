// Node Monitor, part 5 (after 94): with its node gone the monitor is no longer there and has dropped as an item.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var monitorPos = new net.minecraft.core.BlockPos(140, 312, 103);
var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        new net.minecraft.world.phys.AABB(monitorPos).inflate(2.0));
var sb = new StringBuilder("monitorGone=").append(level.getBlockState(monitorPos).isAir()).append(" drops=");
for (var drop : drops) {
    sb.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(drop.getItem().getItem())).append(' ');
}
return sb.toString();
