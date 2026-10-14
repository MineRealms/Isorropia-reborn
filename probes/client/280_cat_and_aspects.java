// S4 compat test: the catalogue item must be gone from the creative tab, its subtypes must name
// themselves, and Thaumcraft's own JEI aspect ingredient must be registered (it needs the newer JEI).
var tab = fr.wind_blade.isorropia.common.IsorropiaCreativeTabs.MAIN.get();
var items = tab.getDisplayItems();
int cats = 0;
for (var s : items) if (s.getItem() == fr.wind_blade.isorropia.common.items.ItemsIS.CAT.get()) cats++;

var cat = fr.wind_blade.isorropia.common.items.misc.ItemCat.class;
var chicken = fr.wind_blade.isorropia.common.items.misc.ItemCat.create(fr.wind_blade.isorropia.common.items.misc.ItemCat.EnumCat.CHICKEN);
var wolf = fr.wind_blade.isorropia.common.items.misc.ItemCat.create(fr.wind_blade.isorropia.common.items.misc.ItemCat.EnumCat.WOLF);
var love = fr.wind_blade.isorropia.common.items.misc.ItemCat.create(fr.wind_blade.isorropia.common.items.misc.ItemCat.EnumCat.LOVE);

String aspectHelper;
var rt = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (rt == null) {
    aspectHelper = "NO_JEI_RUNTIME";
} else {
    try {
        var helper = rt.getIngredientManager().getIngredientHelper(dev.tc4port.thaumcraft.compat.jei.AspectJeiIngredient.TYPE);
        var all = rt.getIngredientManager().getAllIngredients(dev.tc4port.thaumcraft.compat.jei.AspectJeiIngredient.TYPE);
        aspectHelper = (helper == null ? "null" : helper.getClass().getSimpleName()) + " count=" + all.size();
    } catch (RuntimeException e) {
        aspectHelper = "MISSING:" + e.getClass().getSimpleName();
    }
}

return "tabSize=" + items.size() + " catItems=" + cats
        + " | chicken=" + chicken.getHoverName().getString()
        + " wolf=" + wolf.getHoverName().getString()
        + " love=" + love.getHoverName().getString()
        + " | aspect=" + aspectHelper;
