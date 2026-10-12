package fr.wind_blade.isorropia.common.items.crafting;

import fr.wind_blade.isorropia.common.items.ItemFocusIllumination;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * TH's {@code RecipesFocusIlluminationDyes}: one illumination focus and any dyes recolour the focus, the last dye
 * in the grid winning. TH's palette was indexed by the old dye metadata, which runs opposite to {@link DyeColor}.
 */
public class IlluminationDyeRecipe extends CustomRecipe {
    public IlluminationDyeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !result(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return result(container);
    }

    private static ItemStack result(CraftingContainer container) {
        ItemStack focus = ItemStack.EMPTY;
        DyeColor dye = null;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ItemFocusIllumination) {
                if (!focus.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                focus = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                dye = dyeItem.getDyeColor();
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (focus.isEmpty() || dye == null) {
            return ItemStack.EMPTY;
        }
        return ItemFocusIllumination.withColour(focus, paletteIndex(dye));
    }

    /** The 1.7 dye metadata for a colour: black was 0 and white 15, the reverse of the wool order. */
    public static int paletteIndex(DyeColor dye) {
        return 15 - dye.getId();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipesIS.ILLUMINATION_DYE.get();
    }
}
