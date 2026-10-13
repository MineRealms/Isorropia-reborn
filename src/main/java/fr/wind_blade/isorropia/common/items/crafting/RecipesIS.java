package fr.wind_blade.isorropia.common.items.crafting;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Recipe serializers for the crafting-grid recipes that are code rather than data. */
public final class RecipesIS {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Isorropia.MODID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<IlluminationDyeRecipe>> ILLUMINATION_DYE =
            SERIALIZERS.register("illumination_dye", () -> new SimpleCraftingRecipeSerializer<>(IlluminationDyeRecipe::new));

    /** TH's void putty: one damaged item plus putty repairs it, whatever it is. */
    public static final RegistryObject<SimpleCraftingRecipeSerializer<VoidPuttyRepairRecipe>> VOID_PUTTY_REPAIR =
            SERIALIZERS.register("void_putty_repair", () -> new SimpleCraftingRecipeSerializer<>(VoidPuttyRepairRecipe::new));

    private RecipesIS() {
    }
}
