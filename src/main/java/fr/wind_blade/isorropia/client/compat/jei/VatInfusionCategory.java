package fr.wind_blade.isorropia.client.compat.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import dev.tc4port.thaumcraft.compat.jei.AspectJeiIngredient;
import fr.wind_blade.isorropia.common.infusion.InfusionDisplayData;
import fr.wind_blade.isorropia.common.infusion.InfusionJson;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A vat infusion page laid out like Thaumcraft's own infusion page: the pedestal offerings ring
 * the vat, the essentia costs sit in a centred row under it, and the action, target, result,
 * instability and research requirement are labelled around them. Every value comes from the
 * recipe snapshot's raw JSON, and every read is defensive, so a KubeJS-authored recipe can never
 * throw out of this class.
 */
public final class VatInfusionCategory implements IRecipeCategory<InfusionDisplayData.Entry> {
    public static final int WIDTH = 144;
    public static final int HEIGHT = 136;
    private static final int RING_CENTER_X = 50;
    private static final int RING_CENTER_Y = 45;
    private static final int RING_RADIUS = 30;
    private static final int MAX_RING = 8;
    private static final int ASPECT_Y = 84;
    private static final int VIS_Y = 104;
    private static final int INSTABILITY_Y = 114;
    private static final int RESEARCH_Y = 124;
    private static final int ACTION_Y = 0;
    private static final int TARGET_Y = 10;
    private static final int RESULT_Y = 72;
    private static final int LABEL_COLOR = 0xFF3F3F3F;
    private static final int MUTED_COLOR = 0xFF777777;

    private final RecipeType<InfusionDisplayData.Entry> type;
    private final boolean self;
    private final IDrawable background;
    private final IDrawable icon;

    public VatInfusionCategory(IGuiHelper gui, RecipeType<InfusionDisplayData.Entry> type, boolean self) {
        this.type = type;
        this.self = self;
        this.background = VatJeiRender.bookBackground(WIDTH, HEIGHT, VatJeiRender.ALTAR);
        this.icon = gui.createDrawableItemStack(new ItemStack(ItemsIS.MODIFIED_MATRIX.get()));
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
        JsonObject json = entry.json();
        List<Ingredient> components;
        try {
            components = InfusionJson.ingredients(json);
        } catch (RuntimeException malformed) {
            components = List.of();
        }
        int count = Math.min(components.size(), MAX_RING);
        float step = count == 0 ? 0.0F : 360.0F / count;
        float angle = -90.0F;
        for (int i = 0; i < count; i++) {
            int x = RING_CENTER_X + (int) (Mth.cos(angle * Mth.DEG_TO_RAD) * RING_RADIUS) - 8;
            int y = RING_CENTER_Y + (int) (Mth.sin(angle * Mth.DEG_TO_RAD) * RING_RADIUS) - 8;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y + 2)
                    .addIngredients(components.get(i));
            angle += step;
        }
        // The vat itself sits at the ring's centre, where the creature is infused. Thaumcraft's own
        // page draws no slot frames, so the offerings and the vat read as bare icons over the plate.
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, RING_CENTER_X - 8, RING_CENTER_Y - 8)
                .addItemStack(new ItemStack(ItemsIS.MODIFIED_MATRIX.get()));

        List<AspectCost> aspects = aspects(json);
        int aspectStep = VatJeiRender.fittedAspectStep(WIDTH - 4, aspects.size(), VatJeiRender.ASPECT_STEP);
        int aspectX = 2 + VatJeiRender.centeredAspectX(WIDTH - 4, aspects.size(), aspectStep);
        for (int i = 0; i < aspects.size(); i++) {
            AspectCost cost = aspects.get(i);
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, aspectX + i * aspectStep, ASPECT_Y)
                    .addIngredient(AspectJeiIngredient.TYPE, new AspectJeiIngredient(cost.id(), cost.amount()));
        }
    }

    @Override
    public void draw(InfusionDisplayData.Entry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
        JsonObject json = entry.json();
        VatJeiRender.drawCenteredFittedString(gui,
                Component.translatable(self ? "jei.isorropia.self_action" : "jei.isorropia.creature_action"),
                WIDTH / 2, ACTION_Y, LABEL_COLOR, WIDTH - 4);

        Component target = targetLabel(json);
        if (target != null) {
            VatJeiRender.drawCenteredFittedString(gui, target, WIDTH / 2, TARGET_Y, MUTED_COLOR, WIDTH - 4);
        }
        Component result = resultLabel(json);
        if (result != null) {
            VatJeiRender.drawCenteredFittedString(gui, result, WIDTH / 2, RESULT_Y, MUTED_COLOR, WIDTH - 4);
        }
        Component vis = visLabel(json);
        if (vis != null) {
            VatJeiRender.drawCenteredFittedString(gui, vis, WIDTH / 2, VIS_Y, MUTED_COLOR, WIDTH - 4);
        }
        int instability = integer(json, "instability");
        if (instability != 0) {
            VatJeiRender.drawCenteredFittedString(gui,
                    Component.translatable("jei.isorropia.instability", instability), WIDTH / 2, INSTABILITY_Y, MUTED_COLOR, WIDTH - 4);
        }
        VatJeiRender.drawResearch(gui, research(json), WIDTH, RESEARCH_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, InfusionDisplayData.Entry entry, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        VatJeiRender.addResearchTooltip(tooltip, research(entry.json()), WIDTH, RESEARCH_Y, mouseX, mouseY);
    }

    /** What the recipe accepts: a creature id, a tag, or one of the named predicates. */
    private static Component targetLabel(JsonObject json) {
        JsonObject target = object(json, "target");
        if (target == null) {
            return null;
        }
        if (target.has("entity")) {
            return Component.translatable("jei.isorropia.target", string(target, "entity"));
        }
        if (target.has("tag")) {
            return Component.translatable("jei.isorropia.target", "#" + string(target, "tag"));
        }
        return Component.translatable("jei.isorropia.target",
                Component.translatable("jei.isorropia.target." + string(target, "type", "any")));
    }

    /** What the infusion changes: a new species, an organ, or a self infusion named by its research. */
    private static Component resultLabel(JsonObject json) {
        String kind = string(json, "kind", "");
        if (kind.equals("specie")) {
            String result = string(json, "result", "");
            return result.isEmpty() ? null : Component.translatable("jei.isorropia.result", entityName(result));
        }
        if (kind.equals("organ")) {
            String organ = string(json, "organ", "");
            return organ.isEmpty() ? null : Component.translatable("jei.isorropia.organ", Component.translatable("jei.isorropia.organ." + organ));
        }
        // Self infusions carry both an infusion id (with underscores) and the research key that names
        // them; only the research key has a translation, so use that and never the raw infusion id.
        String research = string(json, "research", "");
        return research.isEmpty() ? null : Component.translatable("jei.isorropia.result", Component.translatable("tc.research_name." + research));
    }

    private static Component entityName(String id) {
        try {
            var location = net.minecraft.resources.ResourceLocation.parse(id);
            var entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(location);
            return entityType == null ? Component.literal(id) : entityType.getDescription();
        } catch (RuntimeException unknown) {
            return Component.literal(id);
        }
    }

    /** One muted line of localised channel names, e.g. "Aer 10, Terra 5". */
    private static Component visLabel(JsonObject json) {
        JsonObject vis = object(json, "vis");
        if (vis == null || vis.size() == 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, JsonElement> channel : vis.entrySet()) {
            int amount = amount(channel.getValue());
            if (amount == 0) {
                continue;
            }
            parts.add(aspectName(channel.getKey()) + " " + amount);
        }
        return parts.isEmpty() ? null : Component.literal(String.join(", ", parts));
    }

    private static String aspectName(String serialized) {
        try {
            return AspectApi.tooltipName(AspectId.parse(serialized)).getString();
        } catch (RuntimeException unknown) {
            return serialized;
        }
    }

    /** One essentia cost. The raw JSON key (e.g. {@code thaumcraft:corpus}) is kept so the amount can
     *  be looked up again: {@link AspectId#serialized()} drops the namespace and would miss it. */
    private record AspectCost(AspectId id, int amount) {
    }

    private static List<AspectCost> aspects(JsonObject json) {
        JsonObject aspects = object(json, "aspects");
        if (aspects == null) {
            return List.of();
        }
        List<AspectCost> costs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : aspects.entrySet()) {
            try {
                costs.add(new AspectCost(AspectId.parse(entry.getKey()), amount(entry.getValue())));
            } catch (RuntimeException unknown) {
                // Unknown aspect ids are dropped rather than thrown.
            }
        }
        return costs;
    }

    private static ResearchKey research(JsonObject json) {
        String research = string(json, "research", "");
        if (research.isEmpty()) {
            return null;
        }
        try {
            return ResearchKey.parse(research);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static String string(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null) {
            return "";
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.size() == 0 ? "" : array.get(0).getAsString();
        }
        return element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static int amount(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : 0;
    }

    private static int integer(JsonObject json, String key) {
        return amount(json.get(key));
    }
}
