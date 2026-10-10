package fr.wind_blade.isorropia.client.renderer.entities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityHellHound;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderHellHound extends MobRenderer<EntityHellHound, WolfModel<EntityHellHound>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/hellhound.png");

    public RenderHellHound(EntityRendererProvider.Context context) {
        super(context, new WolfModel<>(context.bakeLayer(ModelLayers.WOLF)), 0.5F);
    }

    @Override
    protected float getBob(EntityHellHound entity, float partialTick) {
        return entity.getTailAngle();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHellHound entity) {
        return TEXTURE;
    }
}
