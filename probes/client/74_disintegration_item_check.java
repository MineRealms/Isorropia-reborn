// Disintegration focus, part 5: the raw iron should be gone, replaced by crystallized essentia whose stack counts
// are the per-unit aspects doubled by the two-item stack. Releases the beam. Broadened for diagnosis: lists every
// dropped item within ten blocks of the player, with positions.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
sb.append("using=").append(player.isUsingItem());
sb.append(" player@").append(player.blockPosition());
var box = player.getBoundingBox().inflate(10.0);
var found = new StringBuilder();
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    var stack = item.getItem();
    found.append(" ").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()))
         .append("x").append(stack.getCount())
         .append("(").append(dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.item(stack).amounts()).append(")")
         .append("@").append(item.blockPosition());
}
sb.append(" drops=").append(found.length() == 0 ? "NONE" : found.toString());
var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();
