package fr.wind_blade.isorropia.common.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
*///?}

/**
 * A special crafting-grid recipe written once for both versions. 1.20.1 hands recipes the live
 * {@code CraftingContainer}; 1.21 hands them an immutable {@code CraftingInput} and drops the recipe id from the
 * constructor. Subclasses only see {@link Grid}, so their matching code is shared; only their constructor stays dual
 * because the serializer factory's shape follows the version.
 */
public abstract class GridRecipe extends CustomRecipe {
    /** The crafting grid, slot by slot, whatever the version's own type for it. */
    public interface Grid {
        int size();

        ItemStack get(int slot);
    }

    //? if forge {
    protected GridRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public final boolean matches(CraftingContainer container, Level level) {
        return matchesGrid(grid(container), level);
    }

    @Override
    public final ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return assembleGrid(grid(container));
    }

    private static Grid grid(CraftingContainer container) {
        return new Grid() {
            @Override
            public int size() {
                return container.getContainerSize();
            }

            @Override
            public ItemStack get(int slot) {
                return container.getItem(slot);
            }
        };
    }
    //?} else {
    /*protected GridRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public final boolean matches(CraftingInput input, Level level) {
        return matchesGrid(grid(input), level);
    }

    @Override
    public final ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return assembleGrid(grid(input));
    }

    private static Grid grid(CraftingInput input) {
        return new Grid() {
            @Override
            public int size() {
                return input.size();
            }

            @Override
            public ItemStack get(int slot) {
                return input.getItem(slot);
            }
        };
    }
    *///?}

    protected abstract boolean matchesGrid(Grid grid, Level level);

    protected abstract ItemStack assembleGrid(Grid grid);
}
