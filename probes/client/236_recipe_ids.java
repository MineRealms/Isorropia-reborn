// Both new recipes must be registered under the ids the research pages reference.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();
try {
    var manager = server.getRecipeManager();
    var arcane = manager.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "arcane/crystal_wand"));
    var repair = manager.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "void_putty_repair"));
    sb.append("arcaneCrystalWand=").append(arcane.isPresent())
            .append(" voidPuttyRepair=").append(repair.isPresent()).append("\n");
    arcane.ifPresent(r -> sb.append("  arcane type=").append(r.getSerializer())
            .append(" result=").append(r.getResultItem(mc.level.registryAccess()).getItem()).append("\n"));
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}
