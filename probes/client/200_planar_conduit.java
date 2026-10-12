// Verify the TH planar conduit port: the item registers, its arcane recipe resolves, and the
// planarTheory research entry loads through TC4R's catalog.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();
var item = fr.wind_blade.isorropia.common.items.ItemsIS.PLANAR_CONDUIT.get();
var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
sb.append("item_id=").append(itemId).append("\n");
sb.append("item_class=").append(item.getClass().getName()).append("\n");
sb.append("item_registered=").append(itemId.toString().equals("isorropia:planar_conduit")).append("\n");

var recipeRl = new net.minecraft.resources.ResourceLocation("isorropia", "arcane/planar_conduit");
var recipe = server.getRecipeManager().byKey(recipeRl);
sb.append("recipe_present=").append(recipe.isPresent()).append("\n");
if (recipe.isPresent()) {
    var r = recipe.get();
    sb.append("recipe_type=").append(r.getType()).append("\n");
    var resultStack = r.getResultItem(server.registryAccess());
    sb.append("recipe_result=").append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(resultStack.getItem())).append("\n");
}

try {
    var key = new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", "planarTheory");
    var entry = dev.tc4port.thaumcraft.research.ResearchCatalog.get(key);
    sb.append("research_present=").append(entry != null).append("\n");
    if (entry != null) {
        sb.append("research_aspects=").append(entry.aspects()).append("\n");
        sb.append("research_parents=").append(entry.parents()).append("\n");
        sb.append("research_pages=").append(entry.pages().size()).append("\n");
    }
} catch (Throwable t) {
    sb.append("research_error=").append(t).append("\n");
}
return sb.toString();
