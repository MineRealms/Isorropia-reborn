package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.common.entities.EntityAnimatedBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Draws a conjured golem as the block it is made of, floating at its chest height with a small walk bob.
 * Thaumic Horizons built a whole block-textured humanoid; this port keeps the block itself, which reads clearly
 * as the animated block the focus conjures without re-creating that model.
 */
public class RenderAnimatedBlock extends EntityRenderer<EntityAnimatedBlock> {
    public RenderAnimatedBlock(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(EntityAnimatedBlock entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        BlockState state = entity.getBlockState();
        if (!state.isAir()) {
            float bob = (float) Math.sin((entity.tickCount + partialTick) * 0.25F) * 0.04F;
            poseStack.pushPose();
            poseStack.translate(0.0D, entity.getBbHeight() * 0.5D + bob, 0.0D);
            float scale = 0.9F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, packedLight,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, ItemBlockRenderTypes.getRenderType(state, false));
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityAnimatedBlock entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
