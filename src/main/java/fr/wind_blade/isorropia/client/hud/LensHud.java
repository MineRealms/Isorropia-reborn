package fr.wind_blade.isorropia.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectQueryApi;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.LensClientState;
import fr.wind_blade.isorropia.common.capabilities.LivingData;
import fr.wind_blade.isorropia.common.lenses.EnvyLens;
import fr.wind_blade.isorropia.common.lenses.Lens;
import fr.wind_blade.isorropia.common.lenses.LensManager;
import fr.wind_blade.isorropia.common.lenses.Lenses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The screen half of the worn lenses: the scholar's reading of whatever the wearer looks at, and the
 * trickster's patience bar. The world-space halves (the ripples, the floating name) live in LensWorldFx.
 *
 * <p>Everything is read from the goggles the client player is wearing, so no extra sync is needed for which
 * lenses are fitted; only the trickster's count comes over the network, on the creature's own data.
 */
public final class LensHud {
    private static final ResourceLocation ENVY_BAR = Isorropia.id("textures/aspects/invidia_hud.png");
    private static final float ENVY_GREEN_R = 0.0F;
    private static final float ENVY_GREEN_G = 186.0F / 255.0F;
    private static final float ENVY_GREEN_B = 0.0F;
    private static final int NAME_COLOUR = 0xF0EE96;
    private static final int AMOUNT_COLOUR = 0xFFFFFF;

    private LensHud() {
    }

    /** Draws both readouts, if the lenses that ask for them are fitted. */
    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!LensManager.isGoggles(head)) {
            return;
        }
        if (fitted(head, Lenses.ORDO)) {
            scholar(graphics, minecraft);
        }
        if (fitted(head, Lenses.ENVY)) {
            envy(graphics, minecraft, EnvyLens.lookedAt(minecraft.player));
        }
    }

    private static boolean fitted(ItemStack head, Lens lens) {
        return Lens.read(head, Lens.Slot.LEFT) == lens || Lens.read(head, Lens.Slot.RIGHT) == lens;
    }

    /**
     * The scholar's reading. A creature's name floats in the world (LensWorldFx), so only its aspects are drawn
     * here; a block has no world name, so its name and aspects both go on the screen, like the thaumometer.
     */
    private static void scholar(GuiGraphics graphics, Minecraft minecraft) {
        int centreX = graphics.guiWidth() / 2;
        int centreY = graphics.guiHeight() / 2;
        Entity target = LensClientState.scholarTarget();
        if (target instanceof LivingEntity living) {
            drawAspects(graphics, minecraft, AspectQueryApi.entity(living), centreX, centreY);
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        // A drop resting on the looked-at block is what the wearer means to study, not the block under it.
        ItemEntity dropped = firstItemAbove(minecraft, hit.getBlockPos());
        Component name;
        AspectAmounts aspects;
        if (dropped != null) {
            name = dropped.getItem().getHoverName();
            aspects = AspectQueryApi.item(dropped.getItem());
        } else {
            BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
            name = state.getBlock().getName();
            // A placed block's aspects are its item's, plus whatever only the block state adds; querying the
            // state alone finds nothing, which is how the original read it too (it asked for the block's item).
            aspects = AspectQueryApi.placedBlock(new ItemStack(state.getBlock()), state);
        }
        graphics.drawCenteredString(minecraft.font, name, centreX, centreY + 35, NAME_COLOUR);
        drawAspects(graphics, minecraft, aspects, centreX, centreY);
    }

    /** The first dropped item resting in the block space above the looked-at block, if any. */
    private static ItemEntity firstItemAbove(Minecraft minecraft, BlockPos pos) {
        List<ItemEntity> items = minecraft.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.above()));
        return items.isEmpty() ? null : items.get(0);
    }

    /** Aspect icons in rows of at most five, each with its count, centred on the crosshair. */
    private static void drawAspects(GuiGraphics graphics, Minecraft minecraft, AspectAmounts aspects,
            int centreX, int centreY) {
        List<Map.Entry<AspectId, Integer>> entries = new ArrayList<>(aspects.amounts().entrySet());
        if (entries.isEmpty()) {
            return;
        }
        int columns = Math.min(5, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int rowCount = Math.min(columns, entries.size() - row * columns);
            int x = centreX - rowCount * 9 + column * 18 + 1;
            int y = centreY - 20 + row * 18;
            ThaumcraftClientRenderApi.drawAspect(graphics, entries.get(i).getKey(), x, y, 1.0F);
            String amount = Integer.toString(entries.get(i).getValue());
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + 15, y + 11, 1.0F);
            pose.scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(minecraft.font, amount, -minecraft.font.width(amount), 0, AMOUNT_COLOUR, true);
            pose.popPose();
        }
    }

    /** The trickster's patience: a bar in the bottom-right that fills as the creature is talked out of its drop. */
    private static void envy(GuiGraphics graphics, Minecraft minecraft, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        LivingData data = LivingData.get(living).orElse(null);
        if (data == null || data.hasLooted()) {
            return;
        }
        float percent = data.getEnvy() / Math.max(1.0F, living.getMaxHealth()) * 5.15F;
        if (percent <= 0.0F || percent > 100.0F) {
            return;
        }
        int x = graphics.guiWidth() - 16;
        int y = graphics.guiHeight() - 16;
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
        graphics.blit(ENVY_BAR, x, y, 16, 8, 0.0F, 0.0F, 32, 16, 32, 16);
        float filled = percent * 8.0F / 100.0F;
        float v = 1.0F - percent / 100.0F;
        graphics.setColor(ENVY_GREEN_R, ENVY_GREEN_G, ENVY_GREEN_B, 1.0F);
        graphics.blit(ENVY_BAR, x, (int) (y + 8.0F - filled), 16, (int) filled, 0.0F, v * 16.0F, 32,
                (int) ((1.0F - v) * 16.0F), 32, 16);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
