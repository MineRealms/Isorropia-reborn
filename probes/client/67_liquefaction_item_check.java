// Liquefaction focus, part 5: the floating raw iron should have been smelted in place to an iron ingot, same count.
// Releases the use key.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
sb.append("using=").append(player.isUsingItem());

var box = new net.minecraft.world.phys.AABB(36, 306, 36, 45, 315, 45);
var found = new StringBuilder();
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    found.append(" ").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()))
         .append("x").append(item.getItem().getCount()).append("@").append(item.position());
}
sb.append(" items=").append(found.length() == 0 ? "NONE" : found.toString());

var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();
