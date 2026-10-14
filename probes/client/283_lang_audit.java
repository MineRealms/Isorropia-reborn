// S4 audit: report any creative-tab item, JEI item ingredient, or JEI infusion label still a raw lang key.
var sb = new StringBuilder();

// 1) creative tab
var tab = fr.wind_blade.isorropia.common.IsorropiaCreativeTabs.MAIN.get();
int badItems = 0;
for (var s : tab.getDisplayItems()) {
    var name = s.getHoverName().getString();
    if (name.contains("isorropia.") || name.startsWith("item.") || name.startsWith("block.") || name.startsWith("entity.")) {
        badItems++;
        if (badItems <= 12) sb.append("TAB[").append(name).append("] ");
    }
}
sb.append("| badItems=").append(badItems).append(" ");

var rt = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (rt == null) return sb + "NO_JEI_RUNTIME";

// 2) JEI item ingredient list (all registered items), filtered to this mod's namespace
int badIngredients = 0;
try {
    for (var s : rt.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
        if (key == null || !key.getNamespace().equals("isorropia")) continue;
        var name = s.getHoverName().getString();
        if (name.contains("isorropia.") || name.startsWith("item.") || name.startsWith("block.")) {
            badIngredients++;
            if (badIngredients <= 12) sb.append("ING[").append(key).append("=").append(name).append("] ");
        }
    }
} catch (RuntimeException e) {
    sb.append("ING_ERR:").append(e).append(" ");
}
sb.append("| badIngredients=").append(badIngredients).append(" ");

// 3) JEI infusion labels via reflection
var cls = fr.wind_blade.isorropia.client.compat.jei.VatInfusionCategory.class;
var mTarget = cls.getDeclaredMethod("targetLabel", com.google.gson.JsonObject.class);
var mResult = cls.getDeclaredMethod("resultLabel", com.google.gson.JsonObject.class);
mTarget.setAccessible(true);
mResult.setAccessible(true);
int badLabels = 0;
var types = java.util.List.of(
        fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.CREATURE,
        fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.SELF);
for (var type : types) {
    for (var e : rt.getRecipeManager().createRecipeLookup(type).get().toList()) {
        for (var m : java.util.List.of(mTarget, mResult)) {
            var c = (net.minecraft.network.chat.Component) m.invoke(null, e.json());
            if (c == null) continue;
            var text = c.getString();
            if (text.contains("isorropia.") || text.contains("tc.research_name") || text.contains("jei.")) {
                badLabels++;
                if (badLabels <= 12) sb.append("LABEL[").append(e.id()).append("=").append(text).append("] ");
            }
        }
    }
}
return sb + "| badLabels=" + badLabels;
