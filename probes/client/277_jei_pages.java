// S4 compat test: deep-link JEI into the creature infusion page and count the recipes
// the plugin registered (19 data-pack ones + the KubeJS-added probe infusion).
var mc = net.minecraft.client.Minecraft.getInstance();
var runtime = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (runtime == null) return "NO_JEI_RUNTIME";

var creatureType = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.CREATURE;
var selfType = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.SELF;
var creatureRecipes = runtime.getRecipeManager().createRecipeLookup(creatureType).get().toList();
var selfRecipes = runtime.getRecipeManager().createRecipeLookup(selfType).get().toList();

var ids = new java.util.ArrayList();
for (var entry : creatureRecipes) ids.add(entry.id().toString());

var types = new java.util.ArrayList();
types.add(creatureType);
mc.execute(() -> runtime.getRecipesGui().showTypes(types));

return "creature=" + creatureRecipes.size() + " self=" + selfRecipes.size() + " ids=" + ids;
