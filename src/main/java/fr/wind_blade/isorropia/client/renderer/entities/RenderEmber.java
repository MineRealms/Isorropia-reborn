package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.projectile.EntityEmber;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A flickering spark sprite from the particle sheet. */
public class RenderEmber extends EntityRenderer<EntityEmber> {
    private static final ResourceLocation PARTICLES = Isorropia.id("textures/misc/particles.png");

    public RenderEmber(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityEmber entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Billboards.drawFrame(poseStack, buffers, this.entityRenderDispatcher, PARTICLES, entity.getBbHeight() / 1.75D, 0.08F, 16,
                151 + entity.tickCount % 7, 0xAADDFF);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEmber entity) {
        return PARTICLES;
    }
}
