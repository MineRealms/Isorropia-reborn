// The curative vat's construction page: grants the research, then opens the Thaumonomicon on the client at the
// blueprint (the book is a client screen over the synced research snapshot). Grab it afterwards with
//   python tools/client_probe.py guishot vat_blueprint
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
var sb = new StringBuilder();
for (String key : new String[]{"isorropia:growth", "isorropia:hardning", "INFUSION", "isorropia:curativevat"}) {
    sb.append(key).append('=').append(dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
            dev.tc4port.thaumcraft.api.research.ResearchKey.parse(key), context)).append(' ');
}
var blueprint = dev.tc4port.thaumcraft.common.CompoundBlueprintCatalog.find(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "compound/curative_vat"));
sb.append("\nblueprint=").append(blueprint == null ? "MISSING" : blueprint.width() + "x" + blueprint.height() + "x"
        + blueprint.depth() + " aspects=" + blueprint.aspects());
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var snapshot = dev.tc4port.thaumcraft.research.ResearchCatalog.snapshot();
        var screen = new dev.tc4port.thaumcraft.client.ResearchBrowserScreen(snapshot);
        mc.setScreen(screen);
        dev.tc4port.thaumcraft.research.ResearchEntryDefinition entry =
                dev.tc4port.thaumcraft.research.ResearchCatalog.get(dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:curativevat"));
        int page = -1;
        for (int i = 0; i < entry.pages().size(); i++) {
            if (entry.pages().get(i).type().name().equals("COMPOUND_CRAFTING")) {
                page = i;
            }
        }
        var cls = dev.tc4port.thaumcraft.client.ResearchBrowserScreen.class;
        var open = cls.getDeclaredField("openResearch");
        open.setAccessible(true);
        open.set(screen, entry);
        var bookPage = cls.getDeclaredField("bookPage");
        bookPage.setAccessible(true);
        bookPage.setInt(screen, page - page % 2);
        done.complete("opened at page " + (page - page % 2) + " (blueprint is page " + page + " of " + entry.pages().size() + ")");
    } catch (Throwable t) {
        done.complete("FAILED " + t);
    }
});
// The client thread does the opening; this only waits for it to report.
sb.append("\n").append(done.get(10, java.util.concurrent.TimeUnit.SECONDS));
return sb.toString();
