package fr.wind_blade.isorropia.common.items.misc;

import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectItemView;
import fr.wind_blade.isorropia.common.IsorropiaAspects;
import fr.wind_blade.isorropia.common.items.AspectNbt;
import fr.wind_blade.isorropia.common.items.IJellyAspectEffectProvider;
import fr.wind_blade.isorropia.common.items.JellyAspectEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Jelly harvested from jelly rabbits: food that carries an aspect, with an aspect-specific effect when eaten. */
public class ItemJelly extends Item implements AspectItemView {
    public static final int ASPECT_AMOUNT = 5;

    public ItemJelly(Properties properties) {
        super(properties.food(food(JellyAspectEffects.DEFAULT, ItemStack.EMPTY)));
    }

    public static ItemStack create(Item item, @Nullable AspectId aspect) {
        ItemStack stack = new ItemStack(item);
        if (aspect != null) {
            AspectNbt.write(stack.getOrCreateTag(), Map.of(aspect, ASPECT_AMOUNT));
        }
        return stack;
    }

    @Nullable
    public static AspectId getAspect(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        return AspectNbt.read(tag).keySet().stream().findFirst().orElse(null);
    }

    private static IJellyAspectEffectProvider provider(ItemStack stack) {
        return JellyAspectEffects.get(getAspect(stack));
    }

    private static FoodProperties food(IJellyAspectEffectProvider provider, ItemStack stack) {
        return new FoodProperties.Builder().nutrition(provider.getNutrition(stack))
                .saturationMod(provider.getSaturationModifier(stack)).alwaysEat().build();
    }

    @Override
    public AspectAmounts storedAspectData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? AspectAmounts.EMPTY : new AspectAmounts(AspectNbt.read(tag));
    }

    @Override
    public Component getName(ItemStack stack) {
        AspectId aspect = getAspect(stack);
        if (aspect == null) {
            return Component.translatable("item.isorropia.jelly.default");
        }
        if (aspect.equals(IsorropiaAspects.VITIUM)) {
            return Component.translatable("item.isorropia.jelly.vitium");
        }
        return Component.translatable("item.isorropia.jelly.aspect", AspectApi.tooltipName(aspect));
    }

    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return food(provider(stack), stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return provider(stack).getEatDuration(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!provider(stack).canBeEaten(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack eaten = stack.copyWithCount(1);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            provider(eaten).onEaten(entity, eaten);
        }
        return result;
    }
}
