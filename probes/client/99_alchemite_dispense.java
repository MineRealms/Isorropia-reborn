// Alchemite from a dispenser, as Thaumic Horizons registered it: a powered dispenser holding one alchemite throws it
// out already lit. The dispenser fires on its scheduled tick, after this probe returns; check with 100.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var dispenserPos = new net.minecraft.core.BlockPos(160, 310, 100);
for (var stale : level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAlchemitePrimed.class,
        new net.minecraft.world.phys.AABB(dispenserPos).inflate(6))) {
    stale.discard();
}
for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
    level.setBlockAndUpdate(dispenserPos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    level.setBlockAndUpdate(dispenserPos.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
}
level.setBlockAndUpdate(dispenserPos, net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState()
        .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.EAST));
var dispenser = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(dispenserPos);
dispenser.setItem(0, new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.ALCHEMITE.get()));
level.setBlockAndUpdate(dispenserPos.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
return "dispenser loaded=" + dispenser.getItem(0) + " powered=" + level.hasNeighborSignal(dispenserPos);
