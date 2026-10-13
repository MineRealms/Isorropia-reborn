package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.BloodInfuserModel;
import fr.wind_blade.isorropia.client.model.ModelSyringe;
import fr.wind_blade.isorropia.common.tiles.TileBloodInfuser;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * TH's {@code TileBloodInfuserRender}: the ten-box chassis drawn through TH's legacy translate plus
 * half-turn, and — whenever the machine holds a syringe — a syringe lying along its side port, turned
 * up on end by the quarter roll TH gave it.
 */
public class RenderBloodInfuser implements BlockEntityRenderer<TileBloodInfuser> {
    private static final ResourceLocation BASE = Isorropia.id("textures/entity/blood_infuser.png");
    private static final ResourceLocation SYRINGE = Isorropia.id("textures/entity/syringe.png");

    private final ModelPart base;
    private final ModelSyringe syringe;

    public RenderBloodInfuser(BlockEntityRendererProvider.Context context) {
        this.base = context.bakeLayer(BloodInfuserModel.LAYER);
        this.syringe = new ModelSyringe(context.bakeLayer(ModelSyringe.LAYER));
    }

    @Override
    public void render(TileBloodInfuser infuser, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        this.base.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(BASE)), light, overlay);
        poseStack.popPose();
        if (infuser.hasBlood()) {
            poseStack.pushPose();
            poseStack.translate(1.90625F, 0.75F, 0.46875F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(SYRINGE));
            this.syringe.body().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
            this.syringe.needle().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
            this.syringe.plungerA().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
            this.syringe.plungerB().render(poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
