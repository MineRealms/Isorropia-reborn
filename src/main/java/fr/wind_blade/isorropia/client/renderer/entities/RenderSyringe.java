package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.ModelSyringe;
import fr.wind_blade.isorropia.common.entities.projectile.EntitySyringe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Thaumic Horizons' thrown syringe. TH turned the model with the syringe's yaw plus a quarter turn and its pitch
 * negated, in the entity's own frame, and tinted the barrel with the injection the syringe carries.
 */
public class RenderSyringe extends EntityRenderer<EntitySyringe> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/syringe.png");
    /** TH drew a thrown syringe at three-quarter alpha. */
    private static final float ALPHA = 0.75F;

    private final ModelSyringe model;

    public RenderSyringe(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new ModelSyringe(context.bakeLayer(ModelSyringe.LAYER));
    }

    @Override
    public void render(EntitySyringe syringe, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, syringe.yRotO, syringe.getYRot()) + 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partialTick, syringe.xRotO, syringe.getXRot())));
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int colour = syringe.getColor();
        RenderColors.part(this.model.body(), poseStack, vertices, light, OverlayTexture.NO_OVERLAY, (colour >> 16 & 255) / 255.0F,
                (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F, ALPHA);
        this.model.needle().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
        this.model.plungerA().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
        this.model.plungerB().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(syringe, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySyringe syringe) {
        return TEXTURE;
    }
}
