package fr.wind_blade.isorropia.client.compat.jei;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.infusion.InfusionDisplayData;
import fr.wind_blade.isorropia.common.infusion.InfusionJson;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One vat infusion page: pedestal components as input slots on the left, the essentia, vis and
 * instability costs as text. Costs are drawn from the recipe snapshot's raw JSON, so this class
 * needs no server-side recipe types (the same reason JEI 15 and 19 differ only in nothing used here).
 */
public final class VatInfusionCategory implements IRecipeCategory<InfusionDisplayData.Entry> {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 80;
    private static final int SLOTS_PER_ROW = 3;
    private static final int MAX_SHOWN = 6;
    private static final int TEXT_X = SLOTS_PER_ROW * 18 + 8;
    private static final int TEXT_COLOR = 0xFF555555;

    private final RecipeType<InfusionDisplayData.Entry> type;
    private final boolean self;
    private final IDrawable background;
    private final IDrawable icon;

    public VatInfusionCategory(IGuiHelper gui, RecipeType<InfusionDisplayData.Entry> type, boolean self) {
        this.type = type;
        this.self = self;
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(BlocksIS.CURATIVE_VAT.get()));
    }

    @Override
    public RecipeType<InfusionDisplayData.Entry> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(self ? "jei.isorropia.self_infusions" : "jei.isorropia.creature_infusions");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionDisplayData.Entry entry, IFocusGroup focuses) {
        List<Ingredient> components = InfusionJson.ingredients(entry.json());
        int shown = Math.min(components.size(), MAX_SHOWN);
        for (int i = 0; i < shown; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, (i % SLOTS_PER_ROW) * 18 + 1, (i / SLOTS_PER_ROW) * 18 + 1)
                    .setStandardSlotBackground()
                    .addIngredients(components.get(i));
        }
    }

    @Override
    public void draw(InfusionDisplayData.Entry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int y = 1;
        for (Component line : costLines(entry.json())) {
            if (y > HEIGHT - font.lineHeight) {
                gui.drawString(font, Component.literal("..."), TEXT_X, y, TEXT_COLOR, false);
                break;
            }
            gui.drawString(font, line, TEXT_X, y, TEXT_COLOR, false);
            y += font.lineHeight + 1;
        }
    }

    private static List<Component> costLines(JsonObject json) {
        List<Component> lines = new ArrayList<>();
        JsonObject aspects = json.getAsJsonObject("aspects");
        if (aspects != null) {
            for (Map.Entry<String, JsonElement> aspect : aspects.entrySet()) {
                lines.add(Component.literal(aspect.getKey() + " x" + aspect.getValue().getAsInt()));
            }
        }
        JsonObject vis = json.getAsJsonObject("vis");
        if (vis != null) {
            for (Map.Entry<String, JsonElement> channel : vis.entrySet()) {
                lines.add(Component.translatable("jei.isorropia.vis",
                        channel.getKey(), channel.getValue().getAsInt()));
            }
        }
        if (json.has("instability") && json.get("instability").getAsInt() != 0) {
            lines.add(Component.translatable("jei.isorropia.instability", json.get("instability").getAsInt()));
        }
        return List.copyOf(lines);
    }
}
