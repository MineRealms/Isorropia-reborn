// Follow-up to 287: the *_fm recipes and the default recipes both reported present, which would mean
// the not(mod_loaded forbidden_magic) condition on the defaults is not being honoured for recipes.
// Dump the actual recipe class and, for a Forge conditional wrapper, its active inner recipe count.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var rm = server.getRecipeManager();
var sb = new StringBuilder();
for (var id : new String[]{"isorropia:infusion/lens_envy", "isorropia:infusion/lens_envy_fm",
                           "isorropia:infusion/somatic_brain", "isorropia:infusion/somatic_brain_fm"}) {
    var opt = rm.byKey(net.minecraft.resources.ResourceLocation.parse(id));
    if (opt.isEmpty()) { sb.append(id).append("=ABSENT\n"); continue; }
    var r = opt.get();
    sb.append(id).append(" class=").append(r.getClass().getName());
    if (r instanceof net.minecraftforge.common.crafting.ConditionalRecipe) {
        try {
            var m = r.getClass().getMethod("getRecipes");
            m.setAccessible(true);
            var inner = (java.util.List<?>) m.invoke(r);
            sb.append(" inner=").append(inner.size());
        } catch (Exception e) {
            sb.append(" inner=?(").append(e.getClass().getSimpleName()).append(")");
        }
    }
    sb.append("\n");
}
return sb.toString();
