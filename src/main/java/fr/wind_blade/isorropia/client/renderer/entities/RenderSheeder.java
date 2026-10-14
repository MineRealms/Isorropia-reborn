package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntitySheeder;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Spider model with spider eyes and, until shorn, a slightly inflated wool overlay. */
public class RenderSheeder extends MobRenderer<EntitySheeder, SpiderModel<EntitySheeder>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/sheeder.png");
    private static final ResourceLocation WOOL = Isorropia.id("textures/entity/sheederoverlay.png");

    public RenderSheeder(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
        this.addLayer(new Wool(this));
    }

    @Override
    protected float getFlipDegrees(EntitySheeder entity) {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySheeder entity) {
        return TEXTURE;
    }

    private static final class Wool extends RenderLayer<EntitySheeder, SpiderModel<EntitySheeder>> {
        Wool(RenderLayerParent<EntitySheeder, SpiderModel<EntitySheeder>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, EntitySheeder entity, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isSheared() || entity.isInvisible()) {
                return;
            }
            poseStack.pushPose();
            RenderColors.render(this.getParentModel(), poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(WOOL)), light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
