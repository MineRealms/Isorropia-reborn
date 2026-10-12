// Vis Dynamo selection screen, part 2: the client shows TH's screen with ignis drawn off, and a click on the aer
// icon goes out as a menu button. The server's answer is read in 116.
var mc = net.minecraft.client.Minecraft.getInstance();
var result = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        if (!(mc.screen instanceof fr.wind_blade.isorropia.client.gui.VisDynamoScreen screen)) {
            result.complete("screen=" + mc.screen);
            return;
        }
        var menu = screen.getMenu();
        boolean ignisShownOff = !menu.enabled(dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS);
        // The aer icon is TH's first switch, sixteen pixels square at (11, 12) inside the panel.
        boolean clicked = screen.mouseClicked(screen.getGuiLeft() + 19, screen.getGuiTop() + 20, 0);
        result.complete("screen=VisDynamoScreen ignisShownOff=" + ignisShownOff + " clicked=" + clicked);
    } catch (Throwable t) {
        result.complete("FAILED " + t);
    }
});
return result.get(10, java.util.concurrent.TimeUnit.SECONDS);
