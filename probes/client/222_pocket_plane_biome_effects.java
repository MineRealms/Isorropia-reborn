// Pocket-plane biomes and effects: a plane made of taint and motion should choose the tainted pocket biome and hand out
// speed + strength, the chooser should fall back to the neutral / magical biomes for the other aspect sets, and the
// stamped chunk should actually report the chosen biome once the biome write has run.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level\n";
}

var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(terra, 20);
aspects.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:vitium"), 10);
aspects.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:motus"), 10);
aspects.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:telum"), 10);

// The chooser: taint first, then the magic pair, then the neutral climate fallback.
var magical = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
magical.put(terra, 10);
magical.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:praecantatio"), 10);
var neutral = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
neutral.put(terra, 10);
var cold = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
cold.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:gelum"), 10);

String chosen = fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(aspects).location().toString();
sb.append("chosen=").append(chosen).append("\n");
sb.append("magical=").append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(magical).location())
        .append("\n");
sb.append("neutral=").append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(neutral).location())
        .append("\n");
sb.append("coldBiomeFromColdAspects=")
        .append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(cold)).append("\n");
sb.append("coldBiomeFromNeutralAspects=")
        .append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(neutral)).append("\n");
sb.append("coldBiomeFromTaintedAspects=")
        .append(fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(aspects)).append("\n");

// The effects TH derived from the same aspects, in TH's order: motus -> speed, telum -> strength.
var effects = fr.wind_blade.isorropia.common.world.PocketPlaneEffects.forAspects(aspects);
var effectNames = new java.util.ArrayList<String>();
for (var effect : effects) {
    effectNames.add(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect).toString());
}
sb.append("effects=").append(effectNames).append("\n");
boolean effectsMatch = effects.equals(java.util.List.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST));
sb.append("effectsMatch=").append(effectsMatch).append("\n");
sb.append("emptyEffects=").append(
        fr.wind_blade.isorropia.common.world.PocketPlaneEffects.forAspects(neutral).isEmpty()).append("\n");

// Stamp the plane, then write the chosen biome into the chunks the stamping loaded.
var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Probe Biome", aspects,
        net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
var centre = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(id);
int radius = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects);
sb.append("id=").append(id).append(" radius=").append(radius).append("\n");

int written = fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.apply(pocket, centre, radius,
        fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(aspects));
sb.append("chunksWritten=").append(written).append("\n");

var stored = pocket.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(centre.getX()),
        net.minecraft.core.QuartPos.fromBlock(centre.getY()), net.minecraft.core.QuartPos.fromBlock(centre.getZ()));
String storedKey = stored.unwrapKey().map(key -> key.location().toString()).orElse("none");
sb.append("centreBiome=").append(storedKey).append("\n");
boolean biomeApplied = storedKey.equals(chosen);
sb.append("biomeApplied=").append(biomeApplied).append("\n");

boolean pass = biomeApplied && written > 0 && effectsMatch
        && fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(cold)
        && !fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(neutral)
        && !fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.isCold(aspects)
        && fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(magical).location().getPath()
                .equals("pocket_plane_magical")
        && fr.wind_blade.isorropia.common.world.PocketPlaneBiomes.choose(neutral).location().getPath()
                .equals("pocket_plane");
sb.append("PASS=").append(pass).append("\n");
return sb.toString();
