package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityGravekeeper;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Ocelot-layout feline; tamed gravekeepers are drawn at 80% like 1.12 tamed ocelots. */
public class RenderGravekeeper extends MobRenderer<EntityGravekeeper, CatModel<EntityGravekeeper>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/gravekeeper.png");

    public RenderGravekeeper(EntityRendererProvider.Context context) {
        super(context, new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGravekeeper entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(EntityGravekeeper entity, PoseStack poseStack, float partialTick) {
        super.scale(entity, poseStack, partialTick);
        if (entity.isTame()) {
            poseStack.scale(0.8F, 0.8F, 0.8F);
        }
    }
}
