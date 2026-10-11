package fr.wind_blade.isorropia.client.fx;

import dev.tc4port.thaumcraft.api.client.ActionPresentationContext;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientPresentationApi;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.common.focus.ContainmentFocus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The containment focus' beam: Thaumcraft 4's continuous wand beam in Thaumic Horizons' blue, aimed at the middle
 * of the creature being held.
 *
 * <p>Thaumic Horizons also meant to open a fading eldritch portal over a creature as it was jarred, but its
 * particle added its quad outside a tessellator batch and the next {@code startDrawingQuads} discarded it, so the
 * portal never appeared; the 1.12 effect drew nothing at all. Capture is therefore heard, not seen, as it was.
 */
public final class ContainmentFx {
    /** Thaumic Horizons' beam colour, 4482815. */
    private static final int BEAM_RGB = 0x4466FF;

    private ContainmentFx() {
    }

    public static void register() {
        ThaumcraftClientPresentationApi.register(ContainmentFocus.ACTION, ContainmentFx::beam);
    }

    /** The beam for one tick of the hold, ending in an impact flare when it meets a creature or a block. */
    private static void beam(ActionPresentationContext context) {
        LivingEntity source = context.source();
        if (source == null) {
            return;
        }
        Vec3 target = context.targetEntity().map(entity -> entity.getBoundingBox().getCenter()).orElse(context.target());
        boolean impact = context.targetEntity().isPresent()
                || source.getEyePosition().distanceTo(context.target()) < ContainmentFocus.RANGE - 0.01;
        ThaumcraftClientRenderApi.showFocusBeam(ContainmentFocus.ACTION, source, context.hand(), target, BEAM_RGB,
                impact ? 2.0F : 0.0F, impact ? 5 : 0);
    }
}
