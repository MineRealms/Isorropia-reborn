// Liquefaction focus, part 3: the iron ore the beam was held on should be gone, with an iron ingot dropped where
// it stood. Releases the use key so the beam stops.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var target = new net.minecraft.core.BlockPos(40, 311, 43);
var state = level.getBlockState(target);
sb.append("target_block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));

var box = new net.minecraft.world.phys.AABB(
        target.getX() - 3, target.getY() - 3, target.getZ() - 3,
        target.getX() + 4, target.getY() + 4, target.getZ() + 4);
var dropped = new StringBuilder();
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    dropped.append(" ").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()))
           .append("x").append(item.getItem().getCount());
}
sb.append(" dropped=").append(dropped.length() == 0 ? "NONE" : dropped.toString());

var use = mc.options.keyUse.getKey();
mc.execute(() -> {
    net.minecraft.client.KeyMapping.set(use, false);
});
return sb.toString();
