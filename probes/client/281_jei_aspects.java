// S4 compat test: open an infusion page that has essentia costs, so a screenshot shows the aspect
// slots rendering with Thaumcraft's own renderer (which only works once its JEI plugin loads).
var WANT = "isorropia:ender_heart";
var mc = net.minecraft.client.Minecraft.getInstance();
var runtime = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (runtime == null) return "NO_JEI_RUNTIME";

var type = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.CREATURE;
var recipes = runtime.getRecipeManager().createRecipeLookup(type).get().toList();
var want = new net.minecraft.resources.ResourceLocation(WANT);
fr.wind_blade.isorropia.common.infusion.InfusionDisplayData.Entry entry = null;
for (var candidate : recipes) if (candidate.id().equals(want)) entry = candidate;
if (entry == null) return "NOT_FOUND " + WANT + " have=" + recipes.size();

var category = new fr.wind_blade.isorropia.client.compat.jei.VatInfusionCategory(
        runtime.getJeiHelpers().getGuiHelper(), type, false);
var list = new java.util.ArrayList();
list.add(entry);
var empty = new java.util.ArrayList();
mc.execute(() -> runtime.getRecipesGui().showRecipes(category, list, empty));
return "showing " + WANT + " json=" + entry.json();
