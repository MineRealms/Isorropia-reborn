package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.JellyColors;
import fr.wind_blade.isorropia.client.model.ModelJelly;
import fr.wind_blade.isorropia.client.model.ModelJellyRabbit;
import fr.wind_blade.isorropia.common.entities.EntityJellyRabbit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** A tinted rabbit core inside a translucent jelly coat that swells with every layer grown. */
public class RenderJellyRabbit extends MobRenderer<EntityJellyRabbit, ModelJellyRabbit> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/rabbit/white.png");
    private static final ResourceLocation JELLY = Isorropia.id("textures/entity/jelly_rabbit.png");

    public RenderJellyRabbit(EntityRendererProvider.Context context) {
        super(context, new ModelJellyRabbit(context.bakeLayer(ModelJellyRabbit.LAYER)), 0.3F);
        this.addLayer(new Jelly(this, new ModelJelly(context.bakeLayer(ModelJelly.LAYER))));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityJellyRabbit entity) {
        return TEXTURE;
    }

    private static final class Jelly extends RenderLayer<EntityJellyRabbit, ModelJellyRabbit> {
        private final ModelJelly model;

        Jelly(RenderLayerParent<EntityJellyRabbit, ModelJellyRabbit> parent, ModelJelly model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, EntityJellyRabbit rabbit, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (rabbit.isInvisible()) {
                return;
            }
            int color = JellyColors.of(rabbit);
            float size = rabbit.getJellySize() / 25.0F + 1.0F;
            float offset = rabbit.getBbHeight() * (size - 1.0F) / 2.0F;
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.5F + offset, 0.0F);
            poseStack.scale(size, size, size);
            poseStack.translate(0.0F, -1.5F, 0.0F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            this.model.prepareMobModel(rabbit, limbSwing, limbSwingAmount, partialTick);
            this.model.setupAnim(rabbit, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(JELLY)), light,
                    LivingEntityRenderer.getOverlayCoords(rabbit, 0.0F), (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                    (color & 255) / 255.0F, JellyColors.ALPHA);
            poseStack.popPose();
        }
    }
}
