package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.common.entities.EntityHangingLabel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Half-block TC jar label flat against the wall, with the aspect icon (tinted with its colour) on top. */
public class RenderHangingLabel extends EntityRenderer<EntityHangingLabel> {
    private static final ResourceLocation LABEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/label.png");
    private static final float LABEL_HALF = 0.25F;
    private static final float ICON_HALF = 8.0F * 0.021F;

    public RenderHangingLabel(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityHangingLabel entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        quad(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(LABEL)), -0.03F, LABEL_HALF, 0xFFFFFF, 1.0F, light);
        AspectId aspect = entity.getAspect();
        AspectDefinition definition = aspect == null ? null : AspectApi.get(aspect);
        if (definition != null) {
            quad(poseStack, buffers.getBuffer(RenderType.entityTranslucent(definition.texture())), -0.031F, ICON_HALF,
                    definition.color(), 1.0F, light);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    /** A square on the plane {@code z}, facing -Z (away from the wall), upright for the viewer. */
    private static void quad(PoseStack poseStack, VertexConsumer consumer, float z, float half, int rgb, float alpha, int light) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        int a = (int) (alpha * 255.0F);
        vertex(consumer, matrix, normal, half, -half, z, 0.0F, 1.0F, r, g, b, a, light);
        vertex(consumer, matrix, normal, -half, -half, z, 1.0F, 1.0F, r, g, b, a, light);
        vertex(consumer, matrix, normal, -half, half, z, 1.0F, 0.0F, r, g, b, a, light);
        vertex(consumer, matrix, normal, half, half, z, 0.0F, 0.0F, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u, float v,
            int r, int g, int b, int a, int light) {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHangingLabel entity) {
        return LABEL;
    }
}
