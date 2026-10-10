package fr.wind_blade.isorropia.client.renderer.entities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityOrePig;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;

public class RenderOrePig extends MobRenderer<EntityOrePig, PigModel<EntityOrePig>> {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/oreboar.png");
    private static final ResourceLocation SADDLE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig_saddle.png");

    public RenderOrePig(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.7F);
        this.addLayer(new SaddleLayer<>(this, new PigModel<>(context.bakeLayer(ModelLayers.PIG_SADDLE)), SADDLE));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOrePig entity) {
        return TEXTURE;
    }
}
