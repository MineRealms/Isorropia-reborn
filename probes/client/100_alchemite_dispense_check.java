// Run after 99: the dispenser is empty and a lit alchemite sits in front of it. The primed alchemite is taken away
// again before it can go off, so the test leaves the area as it found it.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var dispenserPos = new net.minecraft.core.BlockPos(160, 310, 100);
var dispenser = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(dispenserPos);
var primed = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAlchemitePrimed.class,
        new net.minecraft.world.phys.AABB(dispenserPos).inflate(4));
var sb = new StringBuilder("dispenserEmpty=").append(dispenser.isEmpty()).append(" primed=").append(primed.size());
for (var entity : primed) {
    sb.append(" at=").append(entity.blockPosition().toShortString()).append(" fuse=").append(entity.getFuse());
    entity.discard();
}
level.setBlockAndUpdate(dispenserPos.west(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
return sb.toString();
