// Forbidden Magic owns the seven sin aspects, so with FM installed Isorropia must not register its
// own copies (that produced "Ambiguous aspect combination") and every sin reference in our data must
// resolve to forbidden_magic:* instead. This probe runs inside a world with the whole TC4R addon set
// and asserts the registry, the research catalog and the recipe manager all agree.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

java.util.function.Function<String, Boolean> has = (s) ->
        dev.tc4port.thaumcraft.api.aspect.AspectApi.contains(
                dev.tc4port.thaumcraft.api.aspect.AspectId.parse(s));

// --- the sin aspects resolve to FM, ours are gone ---
sb.append("fm_desidia=").append(has.apply("forbidden_magic:desidia"))
  .append(" fm_invidia=").append(has.apply("forbidden_magic:invidia"))
  .append(" fm_gula=").append(has.apply("forbidden_magic:gula"))
  .append(" fm_ira=").append(has.apply("forbidden_magic:ira"))
  .append(" fm_luxuria=").append(has.apply("forbidden_magic:luxuria"))
  .append(" fm_superbia=").append(has.apply("forbidden_magic:superbia"))
  .append(" fm_infernus=").append(has.apply("forbidden_magic:infernus"));
sb.append("\nisorropia_sins_absent=")
  .append(!has.apply("isorropia:desidia") && !has.apply("isorropia:invidia") && !has.apply("isorropia:gula")
          && !has.apply("isorropia:ira") && !has.apply("isorropia:luxuria") && !has.apply("isorropia:superbia")
          && !has.apply("isorropia:infernus"));

// --- the combination table is unambiguous and points at FM ---
var combo = dev.tc4port.thaumcraft.api.aspect.AspectApi.combination(
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:vinculum"),
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:spiritus"));
sb.append(" desidia_combo=").append(combo.map(Object::toString).orElse("none"));
var combo2 = dev.tc4port.thaumcraft.api.aspect.AspectApi.combination(
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:sensus"),
        dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:fames"));
sb.append("\n invidia_combo=").append(combo2.map(Object::toString).orElse("none"));

// --- research: the FM variants are live, the default ones are gated out ---
var reg = dev.tc4port.thaumcraft.api.research.ResearchApi.registry();
java.util.function.Function<String, Boolean> research = (s) ->
        reg.entries().containsKey(dev.tc4port.thaumcraft.api.research.ResearchKey.parse(s));
sb.append("\nenvylensFm=").append(research.apply("isorropia:envylensFm"))
  .append(" envylens_default=").append(research.apply("isorropia:envylens"))
  .append(" somaticbrainFm=").append(research.apply("isorropia:somaticbrainFm"))
  .append(" somaticbrain_default=").append(research.apply("isorropia:somaticbrain"))
  .append(" research_entries=").append(reg.entries().size());

// --- recipes: the FM variants load, the default ones do not ---
var rm = server.getRecipeManager();
java.util.function.Function<String, Boolean> recipe = (s) ->
        rm.byKey(net.minecraft.resources.ResourceLocation.parse(s)).isPresent();
sb.append("\nlens_envy_fm=").append(recipe.apply("isorropia:infusion/lens_envy_fm"))
  .append(" lens_envy_default=").append(recipe.apply("isorropia:infusion/lens_envy"))
  .append(" somatic_fm=").append(recipe.apply("isorropia:infusion/somatic_brain_fm"))
  .append(" somatic_default=").append(recipe.apply("isorropia:infusion/somatic_brain"));

// --- our own items carry FM's aspect (the *_fm object-aspect overlay) ---
// Note: AspectApi.getItemAspects only reads NBT-stored aspects (AspectItemView), so catalog
// object-aspects must be read through ObjectAspectResolver.getObjectTags instead.
var lensEnvy = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
        net.minecraft.resources.ResourceLocation.parse("isorropia:lens_envy"));
var lensEnvyAspects = dev.tc4port.thaumcraft.common.ObjectAspectResolver.getObjectTags(
        new net.minecraft.world.item.ItemStack(lensEnvy));
sb.append("\nlens_envy_item_aspects=").append(lensEnvyAspects)
  .append(" lens_envy_has_fm_invidia=")
  .append(lensEnvyAspects.containsKey(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("forbidden_magic:invidia")));
var somaticBrain = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
        net.minecraft.resources.ResourceLocation.parse("isorropia:somatic_brain"));
sb.append("\nsomatic_brain_item_aspects=")
  .append(dev.tc4port.thaumcraft.common.ObjectAspectResolver.getObjectTags(
        new net.minecraft.world.item.ItemStack(somaticBrain)));
var chocolateCream = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
        net.minecraft.resources.ResourceLocation.parse("isorropia:chocolate_cream"));
sb.append("\nchocolate_cream_item_aspects=")
  .append(dev.tc4port.thaumcraft.common.ObjectAspectResolver.getObjectTags(
        new net.minecraft.world.item.ItemStack(chocolateCream)));
return sb.toString();
