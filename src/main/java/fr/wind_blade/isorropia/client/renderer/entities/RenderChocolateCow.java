package fr.wind_blade.isorropia.client.renderer.entities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityChocow;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderChocolateCow extends MobRenderer<EntityChocow, CowModel<EntityChocow>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/chocow.png");

    public RenderChocolateCow(EntityRendererProvider.Context context) {
        super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityChocow entity) {
        return TEXTURE;
    }
}
