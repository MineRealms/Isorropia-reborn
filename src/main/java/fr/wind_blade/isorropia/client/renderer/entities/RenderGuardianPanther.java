package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityGuardianPanther;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The 1.12 tamed-ocelot model (80%) doubled in size. */
public class RenderGuardianPanther extends MobRenderer<EntityGuardianPanther, CatModel<EntityGuardianPanther>> {
    private static final ResourceLocation[] SKINS = {
            Isorropia.id("textures/entity/guardian1.png"),
            Isorropia.id("textures/entity/guardian2.png"),
            Isorropia.id("textures/entity/guardian3.png")};

    public RenderGuardianPanther(EntityRendererProvider.Context context) {
        super(context, new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGuardianPanther entity) {
        return SKINS[Math.floorMod(entity.getSkin(), SKINS.length)];
    }

    @Override
    protected void scale(EntityGuardianPanther entity, PoseStack poseStack, float partialTick) {
        super.scale(entity, poseStack, partialTick);
        poseStack.scale(1.6F, 1.6F, 1.6F);
    }
}
