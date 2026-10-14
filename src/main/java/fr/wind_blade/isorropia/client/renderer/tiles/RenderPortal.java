package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.common.tiles.TilePortal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Thaumic Horizons' portal swirl. TH drew a quad of {@code misc/vortex.png}, stepping through the sheet's sixteen
 * frames, and grew it from nothing to full size over the first thirty ticks. The colour tells you where it leads: TH's
 * numeric dimensions ran nether red, overworld cyan and end grey, with anything else yellow, and that reading is kept
 * here against the destination key.
 *
 * <p>Each of the nine surface cells draws its own quad, so the quad is half a block wide and half a block tall at full
 * open: the nine of them tile the three-by-three opening exactly. TH's unit quad was twice that, which would have
 * spilled a block past the frame; its renderer never ran there, because the block never made a tile, so the size is
 * read as the fraction it was plainly meant to be. A gateway with no recorded destination is the world-side end, which
 * leads into a pocket plane rather than a vanilla dimension, so it takes TH's catch-all yellow.</p>
 */
public class RenderPortal implements BlockEntityRenderer<TilePortal> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("isorropia",
            "textures/misc/vortex.png");
    private static final int FRAMES = 16;
    /** TH's grow-in finished at thirty ticks and its narrowest axis at five. */
    private static final float OPEN_TICKS = 30.0F;
    private static final float NARROW_TICKS = 5.0F;
    /** Half a block, so the nine surface quads tile the three-by-three opening. */
    private static final float HALF_CELL = 0.5F;

    public RenderPortal(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TilePortal portal, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
            int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = portal.getLevel();
        if (minecraft.player == null || level == null) {
            return;
        }
        float open = Math.min(OPEN_TICKS, portal.openCount() + partialTick);
        if (open <= 0.0F) {
            return;
        }
        float across = Math.min(NARROW_TICKS, open) / NARROW_TICKS * HALF_CELL;
        float tall = open / OPEN_TICKS * HALF_CELL;
        int colour = colourFor(portal.destination());
        int frame = (int) (System.nanoTime() / 50_000_000L % FRAMES);
        float u0 = frame / (float) FRAMES;
        float u1 = (frame + 1.0F) / FRAMES;

        // A gateway's opening is one block thick, so exactly one horizontal axis has a portal beside it: the surface
        // spans that axis and Y, and the quad lies in that plane. TH's quad was camera-facing, which floats the nine
        // of them off the wall and shades the ones below eye level differently; a flat quad tiles the opening instead.
        BlockPos pos = portal.getBlockPos();
        boolean spansX = isPortal(level, pos.west()) || isPortal(level, pos.east());
        boolean spansZ = isPortal(level, pos.north()) || isPortal(level, pos.south());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        VertexConsumer vertices = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(TEXTURE));
        if (spansX == spansZ) {
            poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        } else if (spansZ) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        quad(vertices, poseStack.last(), across, tall, colour, u0, u1);
        poseStack.popPose();
    }

    private static boolean isPortal(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get());
    }

    /**
     * TH's dimension colours: nether red, overworld cyan, end grey, anything else yellow. A world-side gateway has no
     * destination recorded, and leads into a pocket plane rather than a vanilla dimension, so it is the catch-all.
     */
    private static int colourFor(@javax.annotation.Nullable net.minecraft.resources.ResourceKey<Level> destination) {
        if (destination == null) {
            return 0x80801A;
        }
        if (destination.equals(Level.NETHER)) {
            return 0xFF1A1A;
        }
        if (destination.equals(Level.OVERWORLD)) {
            return 0x1ACCCC;
        }
        if (destination.equals(Level.END)) {
            return 0x404040;
        }
        return 0x80801A;
    }

    /** TH ran the frame strip up the quad and the sheet's height across it; the winding is kept. */
    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, float across, float tall, int colour,
            float u0, float u1) {
        int red = colour >> 16 & 0xFF;
        int green = colour >> 8 & 0xFF;
        int blue = colour & 0xFF;
        vertex(vertices, pose, -across, -tall, red, green, blue, u0, 1.0F);
        vertex(vertices, pose, -across, tall, red, green, blue, u1, 1.0F);
        vertex(vertices, pose, across, tall, red, green, blue, u1, 0.0F);
        vertex(vertices, pose, across, -tall, red, green, blue, u0, 0.0F);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, int red, int green,
            int blue, float u, float v) {
        RenderColors.vertex(vertices, pose, x, y, 0.0F, u, v, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT,
                red, green, blue, 255, 0.0F, 0.0F, 1.0F);
    }
}
