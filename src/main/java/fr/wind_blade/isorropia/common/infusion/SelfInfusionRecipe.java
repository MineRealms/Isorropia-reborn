package fr.wind_blade.isorropia.common.infusion;

import com.google.gson.JsonObject;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A self infusion grown into the reincarnation body in a vat, loaded from
 * {@code data/<ns>/isorropia/self_infusions/*.json}. {@code infusion} names the ability in {@link SelfInfusions}
 * the player gains when reborn in that body.
 */
public record SelfInfusionRecipe(ResourceLocation id, ResourceLocation infusion, @Nullable ResearchKey research,
        List<Ingredient> components, Map<AspectId, Integer> aspects, int instability) {

    public static SelfInfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new SelfInfusionRecipe(id, ResourceLocation.parse(GsonHelper.getAsString(json, "infusion")), InfusionJson.research(json),
                InfusionJson.ingredients(json), InfusionJson.aspects(json), GsonHelper.getAsInt(json, "instability", 0));
    }
}
