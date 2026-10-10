package fr.wind_blade.isorropia.common.infusion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A creature infusion performed in the curative vat, loaded from
 * {@code data/<ns>/isorropia/creature_infusions/*.json}. The three upstream recipe classes map to {@link Kind}:
 * CURATIVE alters the creature in place, SPECIE turns it into another species, ORGAN replaces one of its
 * organs (one infusion per organ). Like upstream, any tameable creature that receives an infusion is tamed by
 * the infuser.
 */
public record CreatureInfusionRecipe(ResourceLocation id, Kind kind, @Nullable ResearchKey research,
        Predicate<LivingEntity> target, @Nullable EntityType<?> result, @Nullable Organ organ, List<Ingredient> components,
        Map<AspectId, Integer> aspects, int instability, Map<VisChannel, Integer> vis, CelestialBody celestialBody,
        int celestialAura, int fluxRejection, List<Modifier> modifiers, @Nullable ResourceLocation hook) {

    public enum Kind {
        CURATIVE, SPECIE, ORGAN
    }

    public record Modifier(Attribute attribute, UUID uuid, String name, double amount, AttributeModifier.Operation operation) {
        public AttributeModifier create() {
            return new AttributeModifier(uuid, name, amount, operation);
        }
    }

    public static CreatureInfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
        Kind kind = Kind.valueOf(GsonHelper.getAsString(json, "kind", "curative").toUpperCase(Locale.ROOT));
        EntityType<?> result = null;
        if (kind == Kind.SPECIE) {
            result = entityType(GsonHelper.getAsString(json, "result"));
        }
        Organ organ = kind == Kind.ORGAN ? Organ.byName(GsonHelper.getAsString(json, "organ")) : null;
        JsonObject celestial = GsonHelper.getAsJsonObject(json, "celestial", new JsonObject());
        List<Modifier> modifiers = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "modifiers", new JsonArray())) {
            JsonObject modifier = element.getAsJsonObject();
            ResourceLocation attributeId = ResourceLocation.parse(GsonHelper.getAsString(modifier, "attribute"));
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute == null) {
                throw new JsonParseException("Unknown attribute " + attributeId);
            }
            modifiers.add(new Modifier(attribute, UUID.fromString(GsonHelper.getAsString(modifier, "uuid")),
                    GsonHelper.getAsString(modifier, "name"), GsonHelper.getAsDouble(modifier, "amount"),
                    InfusionJson.operation(GsonHelper.getAsString(modifier, "operation", "addition"))));
        }
        String hook = GsonHelper.getAsString(json, "hook", "");
        return new CreatureInfusionRecipe(id, kind, InfusionJson.research(json), target(GsonHelper.getAsJsonObject(json, "target")),
                result, organ, InfusionJson.ingredients(json), InfusionJson.aspects(json), GsonHelper.getAsInt(json, "instability", 0),
                InfusionJson.vis(json), CelestialBody.byName(GsonHelper.getAsString(celestial, "body", "none")),
                GsonHelper.getAsInt(celestial, "aura", 0), GsonHelper.getAsInt(json, "flux", 0), List.copyOf(modifiers),
                hook.isEmpty() ? null : ResourceLocation.parse(hook));
    }

    /** {"entity": id | [ids]}, {"tag": entity type tag}, or {"type": "any" | "tameable" | "shearable"}. */
    private static Predicate<LivingEntity> target(JsonObject json) {
        if (json.has("entity")) {
            List<EntityType<?>> types = new ArrayList<>();
            JsonElement element = json.get("entity");
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(e -> types.add(entityType(e.getAsString())));
            } else {
                types.add(entityType(element.getAsString()));
            }
            return entity -> types.contains(entity.getType());
        }
        if (json.has("tag")) {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(GsonHelper.getAsString(json, "tag")));
            return entity -> entity.getType().is(tag);
        }
        return switch (GsonHelper.getAsString(json, "type", "any")) {
            case "any" -> entity -> true;
            case "tameable" -> entity -> entity instanceof TamableAnimal;
            case "shearable" -> entity -> entity instanceof IForgeShearable;
            default -> throw new JsonParseException("Unknown target type " + json.get("type"));
        };
    }

    private static EntityType<?> entityType(String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
            throw new JsonParseException("Unknown entity type " + id);
        }
        return ForgeRegistries.ENTITY_TYPES.getValue(location);
    }
}
