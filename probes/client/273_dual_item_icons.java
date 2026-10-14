var mc = net.minecraft.client.Minecraft.getInstance();
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var target = new com.mojang.blaze3d.pipeline.TextureTarget(256, 256, true, net.minecraft.client.Minecraft.ON_OSX);
        target.bindWrite(true);
        var graphics = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
        int n = 0;
        for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals("isorropia")) continue;
            graphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)),
                    n % 12 * 20, n / 12 * 20);
            n++;
        }
        graphics.flush();
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "dual_item_icons.png", target, message -> {});
        mc.getMainRenderTarget().bindWrite(true);
        target.destroyBuffers();
        done.complete("DUAL_ITEM_ICONS_OK count=" + n);
    } catch (Throwable t) {
        mc.getMainRenderTarget().bindWrite(true);
        done.complete("FAILED " + t);
    }
});
return done.get(30, java.util.concurrent.TimeUnit.SECONDS);
