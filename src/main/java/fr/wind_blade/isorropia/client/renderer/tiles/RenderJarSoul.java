package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.wind_blade.isorropia.common.tiles.TileJarSoul;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the creature kept in a soul jar. The jar itself is the block's own model — this only puts the creature
 * inside it, shrunk to fit and turned to face the way the jar does, as upstream's renderer did.
 */
public class RenderJarSoul implements BlockEntityRenderer<TileJarSoul> {
    /** Small enough to sit inside the jar's glass. */
    private static final float INSIDE_SCALE = 0.25F;
    /** Off the floor of the jar, so the creature is not half buried in it. */
    private static final float FLOOR_OFFSET = 0.01F;

    private final EntityRenderDispatcher dispatcher;

    public RenderJarSoul(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(TileJarSoul jar, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
            int overlay) {
        LivingEntity displayed = jar.getDisplayed();
        if (displayed == null) {
            return;
        }
        float yaw = 180.0F - 90.0F * jar.getBlockState()
                .getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).get2DDataValue();
        poseStack.pushPose();
        poseStack.translate(0.5, FLOOR_OFFSET, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(INSIDE_SCALE, INSIDE_SCALE, INSIDE_SCALE);
        // The jar is glass with light coming through it, so what is inside is drawn full bright.
        displayed.setYRot(0.0F);
        displayed.setXRot(0.0F);
        displayed.yRotO = 0.0F;
        displayed.xRotO = 0.0F;
        dispatcher.render(displayed, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, buffers,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
