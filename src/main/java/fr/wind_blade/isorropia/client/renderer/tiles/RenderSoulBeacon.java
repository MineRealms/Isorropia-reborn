package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.common.tiles.TileSoulBeacon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * The soul beacon's beam. The base is the block's own model; what the renderer adds is the column of light it
 * throws upwards, which is the vanilla beacon beam — the original mod copied that renderer, so using it here is
 * the same picture.
 */
public class RenderSoulBeacon implements BlockEntityRenderer<TileSoulBeacon> {
    private static final float[] COLOUR = {1.0F, 1.0F, 1.0F};
    private static final float RADIUS = 0.2F;
    private static final float GLOW_RADIUS = 0.25F;
    /** The beam reaches this far at full length, as it did in the original mod. */
    private static final float BEAM_HEIGHT = 256.0F;

    public RenderSoulBeacon(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileSoulBeacon beacon, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        // The beacon's own block would block a sky test on itself, so the sky is checked above it.
        if (!beacon.getLevel().canSeeSky(beacon.getBlockPos().above())) {
            return;
        }
        float scale = beacon.beamScale();
        if (scale <= 0.0F) {
            return;
        }
        // Upstream scaled the beam's length, not its texture, so it grows out of the beacon.
        int height = Math.round(BEAM_HEIGHT * scale);
        //? if forge {
        BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                beacon.getLevel().getGameTime(), 0, height, COLOUR, RADIUS, GLOW_RADIUS);
        //?} else {
        /*BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                beacon.getLevel().getGameTime(), 0, height, RenderColors.pack(1.0F, 1.0F, 1.0F, 1.0F), RADIUS,
                GLOW_RADIUS);
        *///?}
    }

    /** The beam is visible from far away, and it reaches past the beacon's own chunk. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(TileSoulBeacon beacon) {
        return true;
    }
}
