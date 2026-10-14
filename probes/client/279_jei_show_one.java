// S4 compat test: open one specific infusion page so a screenshot shows that recipe alone.
// Usage: set WANT to a recipe id. Uses showRecipes with a category built from JEI's own helper.
var WANT = "isorropia:quicksilver_limbs";
var mc = net.minecraft.client.Minecraft.getInstance();
var runtime = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (runtime == null) return "NO_JEI_RUNTIME";

var type = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.CREATURE;
var recipes = runtime.getRecipeManager().createRecipeLookup(type).get().toList();
var want = new net.minecraft.resources.ResourceLocation(WANT);
fr.wind_blade.isorropia.common.infusion.InfusionDisplayData.Entry entry = null;
for (var candidate : recipes) if (candidate.id().equals(want)) entry = candidate;
if (entry == null) return "NOT_FOUND " + WANT;

var category = new fr.wind_blade.isorropia.client.compat.jei.VatInfusionCategory(
        runtime.getJeiHelpers().getGuiHelper(), type, false);
var list = new java.util.ArrayList();
list.add(entry);
var empty = new java.util.ArrayList();
mc.execute(() -> runtime.getRecipesGui().showRecipes(category, list, empty));
return "showing " + WANT + " json=" + entry.json();
