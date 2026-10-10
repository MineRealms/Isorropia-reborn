// Everything the port registers, as the running game sees it: mods, registries, aspects, research, data.
StringBuilder sb = new StringBuilder();
var modList = net.minecraftforge.fml.ModList.get();
sb.append("mods ");
for (String mod : new String[]{"thaumcraft", "curios", "terrablender", "isorropia", "rosetta_remote_debug_bridge"}) {
    sb.append(mod).append("=");
    sb.append(modList.isLoaded(mod) ? modList.getModContainerById(mod).get().getModInfo().getVersion() : "MISSING").append(" ");
}
sb.append("\nitems ");
var ireg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
for (String id : new String[]{"isorropia:lens", "isorropia:lens_air", "isorropia:lens_fire", "isorropia:lens_ordo", "isorropia:lens_envy",
        "isorropia:somatic_brain", "isorropia:jar_soul", "isorropia:jelly", "isorropia:cat", "isorropia:incubated_egg", "isorropia:corpse_effigy",
        "isorropia:modified_matrix", "isorropia:soul_beacon", "isorropia:chocolate", "isorropia:taint_pig_spawn_egg", "isorropia:chocolate_milk_bucket",
        "thaumcraft:warded_jar", "thaumcraft:salis_mundus", "thaumcraft:silverwood_log", "thaumcraft:alchemical_construct", "thaumcraft:crystal_essence",
        "thaumcraft:fire_shard", "thaumcraft:thaumium_ingot", "thaumcraft:arcane_worktable", "thaumcraft:tainted_flesh", "thaumcraft:goggles_of_revealing"}) {
    var rl = net.minecraft.resources.ResourceLocation.parse(id);
    sb.append(ireg.get(rl) == net.minecraft.world.item.Items.AIR ? "MISSING(" + id + ") " : "");
}
sb.append("\nblocks ");
var breg = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
for (String id : new String[]{"isorropia:curative_vat", "isorropia:vat_interior", "isorropia:jar_soul", "isorropia:modified_matrix", "isorropia:soul_beacon",
        "thaumcraft:greatwood_planks", "thaumcraft:alchemical_construct", "thaumcraft:crucible"}) {
    var rl = net.minecraft.resources.ResourceLocation.parse(id);
    sb.append(breg.get(rl) == net.minecraft.world.level.block.Blocks.AIR ? "MISSING(" + id + ") " : "");
}
sb.append("\nblock_entities ");
var bereg = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE;
for (String id : new String[]{"isorropia:curative_vat", "isorropia:modified_matrix"}) {
    var rl = net.minecraft.resources.ResourceLocation.parse(id);
    sb.append(bereg.get(rl) == null ? "MISSING(" + id + ") " : id + " ");
}
sb.append("\nentities ");
var ereg = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
for (String id : new String[]{"isorropia:taint_pig", "isorropia:gravekeeper", "isorropia:sheeder", "isorropia:chromatic_sheep", "isorropia:scholar_chicken",
        "isorropia:ore_pig", "isorropia:jelly_rabbit", "isorropia:hanging_label", "isorropia:chocolate_cow", "isorropia:golden_chicken",
        "isorropia:guardian_panther", "isorropia:nether_hound", "isorropia:ember", "isorropia:incubated_egg"}) {
    var rl = net.minecraft.resources.ResourceLocation.parse(id);
    sb.append(ereg.containsKey(rl) ? "" : "MISSING(" + id + ") ");
}
sb.append("\naspects ");
for (String id : new String[]{"isorropia:invidia", "isorropia:gula", "isorropia:luxuria", "isorropia:infernus", "isorropia:superbia",
        "isorropia:desidia", "isorropia:ira", "thaumcraft:fames", "thaumcraft:corpus", "thaumcraft:auram", "thaumcraft:vitium"}) {
    var aspect = dev.tc4port.thaumcraft.api.aspect.AspectId.parse(id);
    if (!dev.tc4port.thaumcraft.api.aspect.AspectApi.contains(aspect)) {
        sb.append("MISSING(").append(id).append(") ");
    }
}
sb.append("\nresearch ");
var keys = dev.tc4port.thaumcraft.api.research.ResearchApi.registry().entries().keySet().stream()
        .map(Object::toString).filter(k -> k.startsWith("isorropia:")).collect(java.util.stream.Collectors.toList());
sb.append("count=").append(keys.size()).append(" hasCurativeVat=").append(keys.contains("isorropia:curativevat"))
  .append(" hasJellyRabbit=").append(keys.contains("isorropia:jellyrabbit"))
  .append(" hasCreatureInfusions=").append(keys.contains("isorropia:creatureinfusions"));
sb.append("\nrecipes creature=").append(fr.wind_blade.isorropia.common.infusion.InfusionRecipes.creatureRecipes().size());
var rm = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getRecipeManager();
sb.append(" loadedCrucible=").append(rm.getRecipes().stream().filter(r -> net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()) != null && net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()).getNamespace().equals("isorropia")).count());
return sb.toString();
