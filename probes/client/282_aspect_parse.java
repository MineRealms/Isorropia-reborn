// S4 debug: why does the ender_heart page show no aspect slots? Check AspectId.parse on the JSON keys.
var mc = net.minecraft.client.Minecraft.getInstance();
var runtime = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.runtime();
if (runtime == null) return "NO_JEI_RUNTIME";
var type = fr.wind_blade.isorropia.client.compat.jei.HorizonsJeiPlugin.CREATURE;
var recipes = runtime.getRecipeManager().createRecipeLookup(type).get().toList();
var want = new net.minecraft.resources.ResourceLocation("isorropia:ender_heart");
fr.wind_blade.isorropia.common.infusion.InfusionDisplayData.Entry entry = null;
for (var c : recipes) if (c.id().equals(want)) entry = c;
if (entry == null) return "NOT_FOUND";
var json = entry.json();
var asp = json.getAsJsonObject("aspects");
var sb = new StringBuilder();
for (var k : asp.keySet()) {
    try {
        var id = dev.tc4port.thaumcraft.api.aspect.AspectId.parse(k);
        var ing = new dev.tc4port.thaumcraft.compat.jei.AspectJeiIngredient(id, asp.get(k).getAsInt());
        sb.append(k).append("->ser=").append(id.serialized()).append(" amt=").append(ing.amount()).append(";");
    } catch (RuntimeException e) {
        sb.append(k).append("->ERR:").append(e).append(";");
    }
}
return "aspects=" + sb;
