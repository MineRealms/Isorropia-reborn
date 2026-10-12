// Vis Dynamo, part 2: with the client player settled in front of the dynamo, right-click it with the wand. The click
// runs the whole path: WandItem.onItemUseFirst -> BlockVisDynamo.use -> TileVisDynamo.beginCharging. Probe 86 reads
// back what the tile did.
var mc = net.minecraft.client.Minecraft.getInstance();
var use = mc.options.keyUse.getKey();
var sb = new StringBuilder();
sb.append("hit=").append(mc.hitResult);
sb.append(" at=").append(mc.player.blockPosition());
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();
