// Self-contained shard edge cases: inputs, zero-valued node aspects, save/load, real removal and legal drops.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
mc.execute(() -> net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false));
player.stopUsingItem();
var pos = new net.minecraft.core.BlockPos(204, 311, 103);
var block = fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get();
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var fire = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS.aspectId();
var box = new net.minecraft.world.phys.AABB(pos).inflate(3);
level.removeBlock(pos, false);
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) item.discard();
level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
level.setBlock(pos, block.defaultBlockState(), 3);
var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
var crystal = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createCrystalEssence(aer);
var plain = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 5);
boolean rejected = !shard.addEssence(crystal) && crystal.getCount() == 1
        && !shard.addEssence(plain) && plain.getCount() == 5 && shard.capacity().isEmpty();
for (int count : new int[] {64, 64, 2}) {
    var essence = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
            dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(aer, 2));
    essence.setCount(count);
    if (!shard.addEssence(essence) || !essence.isEmpty()) return "FAILED feeding essence";
}
shard.addEssence(dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
        dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(fire, 2)));
shard.addToStore(aer, 7);
var saved = shard.saveWithoutMetadata();
var copy = new fr.wind_blade.isorropia.common.tiles.TileEtherealShard(pos, block.defaultBlockState());
copy.load(saved);
boolean zero = copy.nodeState().currentVis().amounts().containsKey(fire)
        && copy.nodeState().currentVis().amount(fire) == 0;
boolean roundtrip = copy.capacity(aer) == 520 && copy.stored(aer) == 7 && copy.capacity(fire) == 4;
var plannedDrops = shard.essenceDrops();
boolean legal = plannedDrops.stream().allMatch(s -> s.getCount() <= s.getMaxStackSize());
// Same-block state updates must not return the contents. Actual block removal must return them once.
level.setBlock(pos, block.defaultBlockState(), 3);
boolean noEarlyDrops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).isEmpty();
level.destroyBlock(pos, true, player);
int aerCount = 0, fireCount = 0, blockCount = 0;
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    var stack = item.getItem();
    legal &= stack.getCount() <= stack.getMaxStackSize();
    if (stack.is(block.asItem())) blockCount += stack.getCount();
    if (stack.is(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("wisp_essence"))) {
        var aspects = dev.tc4port.thaumcraft.api.aspect.AspectApi.getItemAspects(stack);
        if (aspects.amounts().containsKey(aer)) aerCount += stack.getCount();
        if (aspects.amounts().containsKey(fire)) fireCount += stack.getCount();
    }
    item.discard();
}
return "rejected=" + rejected + " zeroRetained=" + zero
        + " monitorCritical=" + fr.wind_blade.isorropia.common.tiles.TileNodeMonitor.isCritical(copy.nodeState())
        + " roundtrip=" + roundtrip
        + " legalStacks=" + legal + " noEarlyDrops=" + noEarlyDrops
        + " actualAer=" + aerCount + " actualFire=" + fireCount + " actualBlocks=" + blockCount;
