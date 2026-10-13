package fr.wind_blade.isorropia.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.common.entities.AnimatedBlockType;
import fr.wind_blade.isorropia.common.entities.EntityGolemTH;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Draws a working golem as the block it is made of, bobbing at its chest height as it walks — the same convention
 * the animation focus's conjuring uses. A voidling (TH's golem raised from nothing at all) has no block of its own;
 * TH gave it a black humanoid skin, and this port gives it obsidian matter to stand in for the same look.
 */
public class RenderGolemTH extends EntityRenderer<EntityGolemTH> {
    public RenderGolemTH(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(EntityGolemTH entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state.isAir() && entity.getGolemType() == AnimatedBlockType.VOID) {
            state = Blocks.OBSIDIAN.defaultBlockState();
        }
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
    public ResourceLocation getTextureLocation(EntityGolemTH entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
