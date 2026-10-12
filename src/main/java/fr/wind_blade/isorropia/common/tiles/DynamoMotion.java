package fr.wind_blade.isorropia.common.tiles;

import net.minecraft.util.Mth;

/**
 * How a Thaumic Horizons dynamo's rings move, kept on the client: while the dynamo runs they rise three tenths of a
 * block out of the base and start to tumble; when it stops they first stop tumbling, then sink back. All rates are
 * TH's, per tick.
 */
public final class DynamoMotion {
    private static final float RISE_STEP = 0.02F;
    public static final float FULL_RISE = 0.3F;
    private static final float SPIN = 2.0F;
    private static final float WIND_DOWN = 8.0F;

    private float rise;
    private float rotation;
    private float tumble;
    private float lastRise;
    private float lastRotation;
    private float lastTumble;

    public void tick(boolean running) {
        lastRise = rise;
        lastRotation = rotation;
        lastTumble = tumble;
        if (running) {
            if (rise < FULL_RISE) {
                // Clamped, so fifteen float steps land on exactly three tenths rather than a hair under it.
                rise = Math.min(FULL_RISE, rise + RISE_STEP);
            } else {
                tumble += SPIN;
                if (tumble >= 360.0F) {
                    tumble -= 360.0F;
                    lastTumble -= 360.0F;
                }
            }
            rotation += SPIN;
            if (rotation >= 360.0F) {
                rotation -= 360.0F;
                lastRotation -= 360.0F;
            }
        } else if (tumble > 0.0F) {
            tumble = Math.max(0.0F, tumble - WIND_DOWN);
        } else if (rise > 0.0F) {
            rise = Math.max(0.0F, rise - RISE_STEP);
        }
    }

    public float rise(float partialTick) {
        return Mth.lerp(partialTick, lastRise, rise);
    }

    public float rotation(float partialTick) {
        return Mth.lerp(partialTick, lastRotation, rotation);
    }

    public float tumble(float partialTick) {
        return Mth.lerp(partialTick, lastTumble, tumble);
    }

    /** The rings are up, which is when TH lit the lightning between them. */
    public boolean risen() {
        return rise >= FULL_RISE;
    }
}
