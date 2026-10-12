package fr.wind_blade.isorropia.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.client.EffectSpriteSheet;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.menu.VisDynamoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;

/**
 * TH's {@code GuiVisDynamo}: the six primals in two columns, each drawn in its colour while the dynamo offers it
 * and in TC4's near-black when it does not. A click flips one and flashes a particle over the cursor.
 */
public class VisDynamoScreen extends AbstractContainerScreen<VisDynamoMenu> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/gui/guidynamo.png");
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/misc/particles.png");
    /** Where TH drew each switch, in button order. */
    private static final int[][] ICONS = {{11, 12}, {83, 11}, {11, 45}, {83, 45}, {11, 78}, {83, 78}};
    /** Where TH looked for each click; its second row sits two pixels above where it is drawn. */
    private static final int[][] HITS = {{11, 12}, {83, 12}, {11, 43}, {83, 43}, {11, 78}, {83, 78}};
    /** TH's flash lasts eight rendered frames. */
    private static final int FLASH_FRAMES = 8;
    private static final float FLASH_CELL = 0.0624375F;

    private int flashTimer;
    private int flashX;
    private int flashY;
    private int flashColour;

    public VisDynamoScreen(VisDynamoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 111;
        this.imageHeight = 104;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.disableBlend();
    }

    /** TH drew no captions over the dynamo's panel, only the six switches. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < VisDynamoMenu.BUTTONS.length; i++) {
            VisChannel channel = VisDynamoMenu.BUTTONS[i];
            if (this.menu.enabled(channel)) {
                ThaumcraftClientRenderApi.drawAspect(graphics, channel.aspectId(), ICONS[i][0], ICONS[i][1], 1.0F);
            } else {
                drawMonochrome(graphics, AspectApi.get(channel.aspectId()), ICONS[i][0], ICONS[i][1]);
            }
        }
        if (this.flashTimer > 0) {
            this.flashTimer--;
            drawFlash(graphics);
        }
    }

    /** TC4's {@code drawTag} with its black-and-white flag: the icon in 0.1 grey at four fifths alpha. */
    private static void drawMonochrome(GuiGraphics graphics, AspectDefinition definition, int x, int y) {
        if (definition == null) {
            return;
        }
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(0.1F, 0.1F, 0.1F, 0.8F);
        graphics.blit(definition.texture(), x, y, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /**
     * TH's {@code drawFlash}: one sixteen-pixel cell of Thaumcraft's particle sheet in the primal's colour. Its
     * frames step two cells along row eight and run past the sheet's edge, which wrapped in 1.7.
     */
    private void drawFlash(GuiGraphics graphics) {
        float u0 = (0.5F + this.flashTimer / 8.0F) % 1.0F;
        var uv = ThaumcraftClientRenderApi.effectAtlasUv(EffectSpriteSheet.PARTICLES, u0, u0 + FLASH_CELL,
                0.5F, 0.5F + FLASH_CELL);
        float red = (this.flashColour >> 16 & 255) / 255.0F;
        float green = (this.flashColour >> 8 & 255) / 255.0F;
        float blue = (this.flashColour & 255) / 255.0F;
        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PARTICLES);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f pose = graphics.pose().last().pose();
        float x = this.flashX;
        float y = this.flashY;
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(pose, x, y + 16.0F, 0.0F).uv(uv.u1(), uv.v1()).color(red, green, blue, 1.0F).endVertex();
        buffer.vertex(pose, x + 16.0F, y + 16.0F, 0.0F).uv(uv.u1(), uv.v0()).color(red, green, blue, 1.0F).endVertex();
        buffer.vertex(pose, x + 16.0F, y, 0.0F).uv(uv.u0(), uv.v0()).color(red, green, blue, 1.0F).endVertex();
        buffer.vertex(pose, x, y, 0.0F).uv(uv.u0(), uv.v1()).color(red, green, blue, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < HITS.length; i++) {
            int x = (int) mouseX - (this.leftPos + HITS[i][0]);
            int y = (int) mouseY - (this.topPos + HITS[i][1]);
            // TH's test was inclusive on both edges.
            if (x >= 0 && y >= 0 && x <= 16 && y <= 16 && this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                AspectDefinition definition = AspectApi.get(VisDynamoMenu.BUTTONS[i].aspectId());
                this.flashColour = definition == null ? 0xFFFFFF : definition.color();
                this.flashTimer = FLASH_FRAMES;
                this.flashX = (int) mouseX - this.leftPos - 8;
                this.flashY = (int) mouseY - this.topPos - 8;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
