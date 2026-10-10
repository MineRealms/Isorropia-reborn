package fr.wind_blade.isorropia.client.renderer.entities;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Chicken;

/** Vanilla chicken model and wing flap with a custom skin (scholar and golden chickens). */
public class RenderChickenVariant<T extends Chicken> extends MobRenderer<T, ChickenModel<T>> {
    private final ResourceLocation texture;

    public RenderChickenVariant(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.5F);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }

    @Override
    protected float getBob(T entity, float partialTick) {
        float flap = Mth.lerp(partialTick, entity.oFlap, entity.flap);
        float flapSpeed = Mth.lerp(partialTick, entity.oFlapSpeed, entity.flapSpeed);
        return (Mth.sin(flap) + 1.0F) * flapSpeed;
    }
}
