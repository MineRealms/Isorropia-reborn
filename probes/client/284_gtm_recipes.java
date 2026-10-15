// GTM compat: the pocket-plane infusion line must resolve exactly one variant per research page.
// GregTech is absent in this dev instance, so the default recipes must load and the conditional
// gtceu variants must not; the research catalog must likewise expose only the default entry.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();
String[] ids = {"infusion/keystone", "infusion/keystone_gtceu",
                "infusion/keystone_receptacle", "infusion/keystone_receptacle_gtceu"};
for (var p : ids) {
    var rl = new net.minecraft.resources.ResourceLocation("isorropia", p);
    var r = server.getRecipeManager().byKey(rl);
    sb.append(p).append("=").append(r.isPresent());
    if (r.isPresent()) {
        try {
            var rec = (dev.tc4port.thaumcraft.recipe.InfusionRecipe) r.get();
            sb.append("(inst=").append(rec.instability())
              .append(",comps=").append(rec.pedestalComponents().size())
              .append(")");
        } catch (RuntimeException e) {
            sb.append("(cast-fail:").append(e.getClass().getSimpleName()).append(")");
        }
    }
    sb.append("\n");
}
for (var k : new String[]{"planarKeystone", "planarKeystoneGt"}) {
    var key = new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", k);
    var e = dev.tc4port.thaumcraft.research.ResearchCatalog.get(key);
    sb.append("research_").append(k).append("=").append(e != null);
    if (e != null) {
        sb.append("(pages=").append(e.pages().size()).append(")");
    }
    sb.append("\n");
}
return sb.toString();
