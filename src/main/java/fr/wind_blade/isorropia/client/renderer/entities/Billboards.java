package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.client.ClientRenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Camera-facing, additively blended sprite-sheet quad: the 1.20.1 form of TC6's {@code UtilsFX.renderFacingQuad}. */
public final class Billboards {
    private Billboards() {
    }

    static void drawFrame(PoseStack poseStack, MultiBufferSource buffers, EntityRenderDispatcher dispatcher, ResourceLocation sheet,
            double height, float halfSize, int grid, int frame, int rgb) {
        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float u0 = (frame % grid) / (float) grid;
        float v0 = (frame / grid) / (float) grid;
        float u1 = u0 + 1.0F / grid;
        float v1 = v0 + 1.0F / grid;
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        VertexConsumer consumer = buffers.getBuffer(ClientRenderTypes.additiveSprite(sheet));
        vertex(consumer, matrix, normal, -halfSize, -halfSize, u0, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, -halfSize, u1, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, halfSize, u1, v0, r, g, b);
        vertex(consumer, matrix, normal, -halfSize, halfSize, u0, v0, r, g, b);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v, int r, int g, int b) {
        RenderColors.vertex(consumer, matrix, normal, x, y, 0.0F, u, v, OverlayTexture.NO_OVERLAY,
                LightTexture.FULL_BRIGHT, r, g, b, 255, 0.0F, 1.0F, 0.0F);
    }
}
