package fr.wind_blade.isorropia.common.infusion;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.IsorropiaAspects;
import fr.wind_blade.isorropia.common.entities.EntityJellyRabbit;
import fr.wind_blade.isorropia.common.entities.EntityOrePig;
import fr.wind_blade.isorropia.common.entities.OrePigDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Recipe-specific finishing steps that depend on which pedestal items were used (upstream's JellyRabbitRecipe
 * and OreBoarRecipe subclasses), referenced by name from a creature infusion's {@code hook} field.
 */
public final class InfusionHooks {
    public static final ResourceLocation JELLY_RABBIT = Isorropia.id("jelly_rabbit");
    public static final ResourceLocation ORE_PIG = Isorropia.id("ore_pig");

    private static final Map<String, AspectId> SHARDS = Map.of(
            "air_shard", IsorropiaAspects.AER, "fire_shard", IsorropiaAspects.IGNIS,
            "water_shard", IsorropiaAspects.AQUA, "earth_shard", IsorropiaAspects.TERRA,
            "order_shard", IsorropiaAspects.ORDO, "entropy_shard", IsorropiaAspects.PERDITIO);

    private InfusionHooks() {
    }

    public static void run(@Nullable ResourceLocation hook, LivingEntity entity, List<ItemStack> used) {
        if (JELLY_RABBIT.equals(hook) && entity instanceof EntityJellyRabbit rabbit) {
            for (ItemStack stack : used) {
                AspectId aspect = crystalAspect(stack);
                if (aspect != null) {
                    rabbit.setAspect(aspect);
                    return;
                }
            }
        } else if (ORE_PIG.equals(hook) && entity instanceof EntityOrePig pig) {
            for (ItemStack stack : used) {
                for (OrePigDrops.Entry entry : OrePigDrops.entries()) {
                    if (stack.is(oreTag(entry.material()))) {
                        pig.setOre(entry.material());
                        return;
                    }
                }
            }
        }
    }

    /** The aspect a primal shard or a crystallized essence stands for, or null for any other item. */
    @Nullable
    static AspectId crystalAspect(ItemStack stack) {
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        if (!"thaumcraft".equals(id.getNamespace())) {
            return null;
        }
        AspectId shard = SHARDS.get(id.getPath());
        if (shard != null || !"crystal_essence".equals(id.getPath())) {
            return shard;
        }
        // A crystallized essence carries vitreus plus the aspect it was made from.
        return AspectApi.getItemAspects(stack).amounts().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(IsorropiaAspects.VITREUS))
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static TagKey<Item> oreTag(String material) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores/" + material));
    }
}
