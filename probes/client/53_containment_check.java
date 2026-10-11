// Containment focus, part 2 (run some seconds after 52): releases right-click and reports whether the pig was
// jarred, what the jar holds, and what the beam cost.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
var sb = new StringBuilder();
var pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, player.getBoundingBox().inflate(12));
sb.append("pigs nearby=").append(pigs.size()).append(" usingItem=").append(player.isUsingItem());
for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
    var stack = player.getInventory().getItem(slot);
    if (!stack.isEmpty() && !dev.tc4port.thaumcraft.api.wand.WandApi.view(stack).isPresent()) {
        sb.append("\n slot ").append(slot).append(": ").append(stack).append(" ").append(stack.getHoverName().getString());
        if (stack.getTag() != null && stack.getTag().contains("EntityData")) {
            sb.append(" holds=").append(stack.getTag().getCompound("EntityData").getString("id"));
        }
    }
}
var view = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem());
sb.append("\nvis=").append(view.map(v -> v.visCentivis().toString()).orElse("no wand"));
return sb.toString();
