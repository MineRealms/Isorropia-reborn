package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityTaintPig;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Pig model with a pulsing taint rim and a taint-node sprite hovering over it. */
public class RenderTaintPig extends MobRenderer<EntityTaintPig, PigModel<EntityTaintPig>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/taintfeeder.png");
    private static final ResourceLocation EYES = Isorropia.id("textures/entity/taintfeeder_eyes.png");
    private static final ResourceLocation NODES = Isorropia.id("textures/misc/nodes.png");

    public RenderTaintPig(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.5F);
        this.addLayer(new Border(this, new PigModel<>(context.bakeLayer(ModelLayers.PIG_SADDLE))));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTaintPig entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityTaintPig entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        Billboards.drawFrame(poseStack, buffers, this.entityRenderDispatcher, NODES, entity.getBbHeight() / 1.75D, 2.0F, 32,
                192 + entity.tickCount % 32, 0xAADDFF);
    }

    private static final class Border extends RenderLayer<EntityTaintPig, PigModel<EntityTaintPig>> {
        private final PigModel<EntityTaintPig> model;

        Border(RenderLayerParent<EntityTaintPig, PigModel<EntityTaintPig>> parent, PigModel<EntityTaintPig> model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, EntityTaintPig entity, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            float pulse = (float) Math.sin(Math.toRadians(entity.tickCount % 45) * 8.0D);
            this.getParentModel().copyPropertiesTo(this.model);
            this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(EYES)), light,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.32F + 0.32F * pulse);
        }
    }
}
