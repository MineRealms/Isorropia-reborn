package fr.wind_blade.isorropia.client;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * What the scholar's lens is currently reading, shared between the world overlay and the screen readout: the
 * world pass resolves the target each frame and the readout follows the same entity, so the two agree. The
 * world pass owns it because it runs whether or not the HUD is hidden.
 */
public final class LensClientState {
    /** How fast the floating name grows in, matching the original's per-frame step. */
    private static final float NAME_GROWTH = 0.031F;

    @Nullable
    private static Entity scholarTarget;
    private static float nameSize;

    private LensClientState() {
    }

    /** Called each frame with whatever the scholar's lens is reading, or null when it reads nothing. */
    public static void scholarTarget(@Nullable Entity target) {
        if (target != scholarTarget) {
            scholarTarget = target;
            nameSize = 0.0F;
        }
        if (target != null) {
            nameSize = Math.min(1.0F, nameSize + NAME_GROWTH);
        }
    }

    @Nullable
    public static Entity scholarTarget() {
        return scholarTarget;
    }

    /** Zero when nothing is read, growing to one while a target is held. */
    public static float nameSize() {
        return nameSize;
    }
}
