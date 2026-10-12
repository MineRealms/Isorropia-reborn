package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientVisualsApi;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.ClientRenderTypes;
import fr.wind_blade.isorropia.client.fx.VatFx;
import fr.wind_blade.isorropia.client.model.MatrixCubeModel;
import fr.wind_blade.isorropia.common.tiles.TileModifiedMatrix;
import fr.wind_blade.isorropia.common.tiles.TileVat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The modified matrix: eight cubes hanging around the block's centre in the eight corner directions, turning as
 * the vat winds up, each shaking harder the more unstable the infusion gets. While the vat runs, a second pass
 * lights the same cubes in purple, and during an infusion a halo spins around the whole thing.
 *
 * <p>Every number comes from the 1.12 renderer, which is the shipped behaviour for this block: the corners are a
 * quarter block out, the cubes are scaled to 0.45, the winding angles follow the vat's wind-up, and the purple
 * glow pulses between 0.2 and 0.3 alpha.
 */
public class RenderModifiedMatrix implements BlockEntityRenderer<TileModifiedMatrix> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/block/matrix_vat.png");
    private static final float CUBE_SCALE = 0.45F;
    private static final float CORNER_OFFSET = 0.25F;
    /** The most the cubes can be pushed out of place by instability. */
    private static final float MAX_INSTABILITY = 6.0F;
    private static final int CRAFT_COUNT_CAP = 50;

    private final MatrixCubeModel model;

    public RenderModifiedMatrix(BlockEntityRendererProvider.Context context) {
        this.model = new MatrixCubeModel(context.bakeLayer(MatrixCubeModel.LAYER));
    }

    @Override
    public void render(TileModifiedMatrix matrix, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        TileVat vat = matrix.getVat();
        float ticks = Minecraft.getInstance().getCameraEntity() == null ? partialTick
                : Minecraft.getInstance().getCameraEntity().tickCount + partialTick;
        float startUp = vat == null ? 0.0F : vat.getStartUp();
        boolean active = vat != null && vat.isActive();
        int craftCount = vat == null ? 0 : vat.getCraftCount();
        float instability = Math.min(MAX_INSTABILITY, 1.0F + (vat == null ? 0.0F : vat.getRawStability() * 0.66F)
                * ((float) Math.min(craftCount, CRAFT_COUNT_CAP) / CRAFT_COUNT_CAP));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F * startUp));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F * startUp));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * startUp));
        if (vat != null && vat.isExpunging()) {
            poseStack.mulPose(Axis.YP.rotationDegrees((ticks + partialTick) * 30.0F));
            venting(matrix, ticks);
        }

        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer glow = active ? buffers.getBuffer(ClientRenderTypes.additiveGlow(TEXTURE)) : null;
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < 2; c++) {
                    float[] corner = corner(ticks, a, b, c, startUp, instability);
                    poseStack.pushPose();
                    poseStack.translate(corner[0], corner[1], corner[2]);
                    poseStack.mulPose(Axis.XP.rotationDegrees(a > 0 ? 90.0F : 0.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(b > 0 ? 90.0F : 0.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(c > 0 ? 90.0F : 0.0F));
                    poseStack.scale(CUBE_SCALE, CUBE_SCALE, CUBE_SCALE);
                    model.base().render(poseStack, body, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    if (glow != null) {
                        float alpha = (Mth.sin((ticks + a * 2.0F + b * 3.0F + c * 4.0F) / 4.0F) * 0.1F + 0.2F) * startUp;
                        model.overlay().render(poseStack, glow, LightTexture.FULL_BRIGHT, overlay,
                                0.8F, 0.1F, 1.0F, alpha);
                    }
                    poseStack.popPose();
                }
            }
        }
        poseStack.popPose();

        if (vat != null && vat.isInfusing()) {
            // The halo is Thaumcraft's own, and it is drawn at the block's corner, which the pose is back to.
            ThaumcraftClientVisualsApi.renderInfusionHalo(poseStack, buffers, craftCount);
        }
    }

    /** A corner's offset: a quarter block out, plus the shake the current instability earns it. */
    private static float[] corner(float ticks, int a, int b, int c, float startUp, float instability) {
        float amplitude = startUp * instability;
        return new float[] {
                (a == 0 ? -CORNER_OFFSET : CORNER_OFFSET) + Mth.sin((ticks + a * 10.0F) / 15.0F) * 0.01F * amplitude,
                (b == 0 ? -CORNER_OFFSET : CORNER_OFFSET) + Mth.sin((ticks + b * 10.0F) / 14.0F) * 0.01F * amplitude,
                (c == 0 ? -CORNER_OFFSET : CORNER_OFFSET) + Mth.sin((ticks + c * 10.0F) / 13.0F) * 0.01F * amplitude};
    }

    /** While the vat vents flux, the matrix breathes out a plume, every frame as in 1.12. */
    private void venting(TileModifiedMatrix matrix, float ticks) {
        if (matrix.getLevel() instanceof ClientLevel level) {
            VatFx.matrixPlume(level, matrix.getBlockPos());
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileModifiedMatrix matrix) {
        return true;
    }
}
