package fr.wind_blade.isorropia.common.infusion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JSON fields shared by both vat recipe kinds. */
final class InfusionJson {
    private InfusionJson() {
    }

    @Nullable
    static ResearchKey research(JsonObject json) {
        String research = GsonHelper.getAsString(json, "research", "");
        return research.isEmpty() ? null : ResearchKey.parse(research);
    }

    /** Each entry is one pedestal item; {"count": n} on an entry repeats it. */
    static List<Ingredient> ingredients(JsonObject json) {
        List<Ingredient> result = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "components", new JsonArray())) {
            int count = 1;
            JsonElement value = element;
            if (element.isJsonObject() && element.getAsJsonObject().has("count")) {
                count = GsonHelper.getAsInt(element.getAsJsonObject(), "count", 1);
                // The count is ours, not the ingredient's: the vanilla readers reject unknown fields.
                value = element.getAsJsonObject().deepCopy();
                value.getAsJsonObject().remove("count");
            }
            Ingredient ingredient = Ingredient.fromJson(value, false);
            for (int i = 0; i < count; i++) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    static Map<AspectId, Integer> aspects(JsonObject json) {
        Map<AspectId, Integer> result = new LinkedHashMap<>();
        JsonObject aspects = GsonHelper.getAsJsonObject(json, "aspects", new JsonObject());
        for (Map.Entry<String, JsonElement> entry : aspects.entrySet()) {
            int amount = entry.getValue().getAsInt();
            if (amount <= 0) {
                throw new JsonParseException("Aspect amounts must be positive: " + entry.getKey());
            }
            result.merge(AspectId.parse(entry.getKey()), amount, Integer::sum);
        }
        return Collections.unmodifiableMap(result);
    }

    /** Whole vis per primal channel, drained from the vis network at the vat's matrix. */
    static Map<VisChannel, Integer> vis(JsonObject json) {
        Map<VisChannel, Integer> result = new LinkedHashMap<>();
        JsonObject vis = GsonHelper.getAsJsonObject(json, "vis", new JsonObject());
        for (Map.Entry<String, JsonElement> entry : vis.entrySet()) {
            result.put(VisChannel.valueOf(entry.getKey().toUpperCase(Locale.ROOT)), entry.getValue().getAsInt());
        }
        return Collections.unmodifiableMap(result);
    }

    static AttributeModifier.Operation operation(String name) {
        return switch (name) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> throw new JsonParseException("Unknown attribute operation " + name);
        };
    }
}
