package fr.wind_blade.isorropia.common.items.crafting;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * TH's {@code RecipeVoidPuttyRepair}: one damaged, repairable item plus at least one void putty repairs the item
 * outright, whatever its material or enchantments. Void putty is consumed with the grid, so it does not matter how
 * many are present. TH allowed several putty in the grid; the count never changed the result.
 */
public class VoidPuttyRepairRecipe extends CustomRecipe {
    public VoidPuttyRepairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !damagedItem(container).isEmpty() && hasPutty(container);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack damaged = damagedItem(container);
        if (damaged.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack repaired = damaged.copy();
        repaired.setDamageValue(0);
        return repaired;
    }

    /** The single damaged, damageable item in the grid, or empty if there is none or more than one. */
    private static ItemStack damagedItem(CraftingContainer container) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isDamageableItem() && stack.isDamaged()) {
                if (!found.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                found = stack;
            } else if (!stack.is(ItemsIS.VOID_PUTTY.get())) {
                // Anything that is neither the damaged item nor putty invalidates the recipe.
                return ItemStack.EMPTY;
            }
        }
        return found;
    }

    private static boolean hasPutty(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(ItemsIS.VOID_PUTTY.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipesIS.VOID_PUTTY_REPAIR.get();
    }
}
