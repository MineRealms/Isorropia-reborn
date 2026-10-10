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
