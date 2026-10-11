package fr.wind_blade.isorropia.client.gui;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.menu.MorphicFingersMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The portable arcane workbench the morphic fingers open: a grid, its crystals, and the player's bags. */
public class MorphicFingersScreen extends AbstractContainerScreen<MorphicFingersMenu> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/gui/guifingers.png");

    public MorphicFingersScreen(MorphicFingersMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 190;
        this.imageHeight = 234;
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
