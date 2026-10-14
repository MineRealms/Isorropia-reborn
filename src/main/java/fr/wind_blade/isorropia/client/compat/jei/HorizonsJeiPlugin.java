package fr.wind_blade.isorropia.client.compat.jei;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.infusion.InfusionDisplayData;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public final class HorizonsJeiPlugin implements IModPlugin {
    public static final RecipeType<InfusionDisplayData.Entry> CREATURE =
            RecipeType.create(Isorropia.MODID, "creature_infusions", InfusionDisplayData.Entry.class);
    public static final RecipeType<InfusionDisplayData.Entry> SELF =
            RecipeType.create(Isorropia.MODID, "self_infusions", InfusionDisplayData.Entry.class);
    private IJeiRuntime runtime;
    private List<InfusionDisplayData.Entry> displayed = List.of();
    private static volatile IJeiRuntime activeRuntime;

    @Override
    public ResourceLocation getPluginUid() {
        return Isorropia.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new VatInfusionCategory(gui, CREATURE, false), new VatInfusionCategory(gui, SELF, true));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // The curative vat is a multiblock body with no item form, so the blood infuser
        // (the creature line's entry point) and the modified matrix carry the catalysts.
        registration.addRecipeCatalyst(new ItemStack(ItemsIS.MODIFIED_MATRIX.get()), CREATURE, SELF);
        registration.addRecipeCatalyst(new ItemStack(ItemsIS.BLOOD_INFUSER.get()), CREATURE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        displayed = InfusionDisplayData.entries();
        registration.addRecipes(CREATURE, select(displayed, false));
        registration.addRecipes(SELF, select(displayed, true));
        registration.addItemStackInfo(new ItemStack(ItemsIS.BLOOD_INFUSER.get()), Component.translatable("jei.isorropia.blood_infuser.info"));
        registration.addItemStackInfo(new ItemStack(ItemsIS.SYRINGE_INJECTION.get()), Component.translatable("jei.isorropia.injection.info"));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        activeRuntime = runtime;
        InfusionDisplayData.listen(this::refresh);
        refresh();
    }

    /** Client-side handle for tests and deep links; null until JEI is up. */
    public static IJeiRuntime runtime() {
        return activeRuntime;
    }

    private void refresh() {
        if (runtime == null || displayed == InfusionDisplayData.entries()) return;
        var manager = runtime.getRecipeManager();
        manager.hideRecipes(CREATURE, select(displayed, false));
        manager.hideRecipes(SELF, select(displayed, true));
        displayed = InfusionDisplayData.entries();
        manager.addRecipes(CREATURE, select(displayed, false));
        manager.addRecipes(SELF, select(displayed, true));
    }

    @Override
    public void onRuntimeUnavailable() {
        InfusionDisplayData.listen(null);
        InfusionDisplayData.accept(List.of());
        displayed = List.of();
        runtime = null;
        activeRuntime = null;
    }

    private static List<InfusionDisplayData.Entry> select(List<InfusionDisplayData.Entry> entries, boolean self) {
        return entries.stream().filter(entry -> entry.self() == self).toList();
    }
}
