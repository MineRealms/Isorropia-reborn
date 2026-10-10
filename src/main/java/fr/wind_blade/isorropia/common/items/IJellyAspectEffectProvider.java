package fr.wind_blade.isorropia.common.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** How a jelly of a given aspect behaves as food. Register with {@link JellyAspectEffects#register}. */
public interface IJellyAspectEffectProvider {
    default void onEaten(LivingEntity eater, ItemStack stack) {
    }

    default boolean canBeEaten(LivingEntity eater, ItemStack stack) {
        return true;
    }

    default int getEatDuration(ItemStack stack) {
        return 32;
    }

    default int getNutrition(ItemStack stack) {
        return 2;
    }

    default float getSaturationModifier(ItemStack stack) {
        return this.getNutrition(stack);
    }
}
