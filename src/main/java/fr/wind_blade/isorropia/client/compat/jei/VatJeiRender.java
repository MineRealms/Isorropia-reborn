package fr.wind_blade.isorropia.client.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * The research-book chrome Thaumcraft's own JEI categories are built from (its helper class is
 * package-private, so this mirrors the parts the infusion page needs: the book overlay blit, the
 * centred-fitted label, the aspect row metrics and the research line with its hover tooltip).
 */
final class VatJeiRender {
    static final int ASPECT_SIZE = 16;
    static final int ASPECT_STEP = 20;
    /** Slice of the book overlay that Thaumcraft's infusion page uses as its altar plate. */
    static final BookSprite ALTAR = bookSpriteScaled(8, 12, 200, 77, 60, 44, 90, 66);

    private static final ResourceLocation BOOK_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook_overlay.png");
    private static final int BOOK_TEXTURE = 256;

    private VatJeiRender() {
    }

    static IDrawable bookBackground(int width, int height, BookSprite... sprites) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                for (BookSprite sprite : sprites) {
                    blit(graphics, x + sprite.x(), y + sprite.y(), sprite);
                }
            }
        };
    }

    static BookSprite bookSpriteScaled(int x, int y, int u, int v, int width, int height, int destWidth, int destHeight) {
        return new BookSprite(x, y, u, v, width, height, destWidth, destHeight, 1.0F);
    }

    static void blit(GuiGraphics graphics, int x, int y, BookSprite sprite) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, sprite.alpha());
        graphics.blit(BOOK_OVERLAY, x, y, sprite.destWidth(), sprite.destHeight(),
                sprite.u(), sprite.v(), sprite.width(), sprite.height(), BOOK_TEXTURE, BOOK_TEXTURE);
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Draws the text centred on {@code centerX}, shrinking it if it would exceed {@code maxWidth}. */
    static void drawCenteredFittedString(GuiGraphics graphics, Component text, int centerX, int y, int color, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        float scale = textWidth <= maxWidth ? 1.0F : (float) maxWidth / (float) textWidth;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    static int centeredAspectX(int width, int count, int step) {
        if (count <= 0) {
            return width / 2;
        }
        int contentWidth = ASPECT_SIZE + (count - 1) * step;
        return Math.max(0, (width - contentWidth) / 2);
    }

    static int fittedAspectStep(int width, int count, int preferredStep) {
        return count < 2 ? preferredStep : Math.min(preferredStep, Math.max(0, width - ASPECT_SIZE) / (count - 1));
    }

    static void drawResearch(GuiGraphics graphics, @Nullable ResearchKey research, int width, int y) {
        if (research == null) {
            return;
        }
        Component name = Component.translatable(research.nameTranslationKey());
        drawCenteredFittedString(graphics, name, width / 2, y, 0x777777, width - 4);
    }

    static void addResearchTooltip(ITooltipBuilder tooltip, @Nullable ResearchKey research, int width, int y, double mouseX, double mouseY) {
        if (research == null || !inside(mouseX, mouseY, 0, y, width, 10)) {
            return;
        }
        tooltip.add(Component.translatable("jei.thaumcraft.required_research").withStyle(ChatFormatting.GOLD));
        Component name = Component.translatable(research.nameTranslationKey());
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            tooltip.add(name);
            return;
        }
        boolean complete = ResearchApi.isComplete(player, research);
        tooltip.add(name.copy().append(" - ")
                .append(Component.translatable(complete ? "jei.thaumcraft.research_complete" : "jei.thaumcraft.research_incomplete"))
                .withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    record BookSprite(int x, int y, int u, int v, int width, int height, int destWidth, int destHeight, float alpha) {
    }
}
