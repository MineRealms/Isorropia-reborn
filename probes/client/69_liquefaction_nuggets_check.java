// Liquefaction focus, part 7: the iron ore should be gone, with an iron ingot and (at least one) bonus iron nugget
// dropped where it stood, proving the nuggets upgrade reaches the infernal-furnace bonus table. Releases the beam.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
sb.append("using=").append(player.isUsingItem());

var target = new net.minecraft.core.BlockPos(40, 311, 43);
sb.append(" target_block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(target).getBlock()));

var box = new net.minecraft.world.phys.AABB(37, 307, 37, 44, 314, 44);
var found = new StringBuilder();
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    found.append(" ").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()))
         .append("x").append(item.getItem().getCount());
}
sb.append(" drops=").append(found.length() == 0 ? "NONE" : found.toString());

var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();
