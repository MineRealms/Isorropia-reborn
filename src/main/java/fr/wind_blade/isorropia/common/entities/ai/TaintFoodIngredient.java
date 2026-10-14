package fr.wind_blade.isorropia.common.entities.ai;

import fr.wind_blade.isorropia.common.compat.TaintBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/** Runtime-only ingredient for the taint pig's tempt goal: bottled taint or any taint block. */
//? if forge {
public class TaintFoodIngredient extends Ingredient {
    private static final ResourceLocation BOTTLED_TAINT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "bottled_taint");

    public TaintFoodIngredient() {
        super(Stream.empty());
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (BOTTLED_TAINT.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
            return true;
        }
        return stack.getItem() instanceof BlockItem block && TaintBridge.isTaint(block.getBlock().defaultBlockState());
    }

    @Override
    public boolean isSimple() {
        return false;
    }
}
//?} else {
/*public class TaintFoodIngredient implements net.neoforged.neoforge.common.crafting.ICustomIngredient {
    private static final ResourceLocation BOTTLED_TAINT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "bottled_taint");

    // The pig only ever uses this as a live predicate, never serialized, so the type codec is a stub.
    public static final net.neoforged.neoforge.common.crafting.IngredientType<TaintFoodIngredient> TYPE =
            new net.neoforged.neoforge.common.crafting.IngredientType<>(
                    com.mojang.serialization.MapCodec.unit(TaintFoodIngredient::new));

    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (BOTTLED_TAINT.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return true;
        }
        return stack.getItem() instanceof BlockItem block && TaintBridge.isTaint(block.getBlock().defaultBlockState());
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.empty();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public net.neoforged.neoforge.common.crafting.IngredientType<?> getType() {
        return TYPE;
    }
}
*///?}
