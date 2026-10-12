package fr.wind_blade.isorropia.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * The 1.12 vat and matrix particle effects, rebuilt on {@link GenericFx} with the original numbers: the celestial
 * absorption that rains light into a vat drawing on the sun or moon, the flux explosion of a vat tearing apart,
 * and the plume a venting matrix breathes out.
 */
public final class VatFx {
    /** 1.12 passed (0.49, 0.557, 0.635) through setRBGColorF(red, blue, green), so the motes are this colour. */
    private static final float AURA_RED = 0.49019608F;
    private static final float AURA_GREEN = 0.63529414F;
    private static final float AURA_BLUE = 0.5568628F;
    /**
     * The plume's cell. 1.12 asked for TC6's vent cloud (cell 56 of its sixteen-wide grid), which Isorropia never
     * shipped; the sheet's white cloud stands in, so the pink tint shows as it did on TC6's light steam.
     */
    private static final int CLOUD_FRAME = 1;

    private VatFx() {
    }

    /**
     * FXCelestialAbsorption: fifty motes a tick. Absorbing, they hang in a column above the vat and sink into it,
     * one in five a sparkle that floats; giving the aura back, they rise out of the vat instead.
     */
    public static void celestialAbsorption(ClientLevel level, double x, double y, double z, boolean absorb) {
        RandomSource random = level.random;
        for (int i = 0; i < 50; i++) {
            boolean sparkle = random.nextFloat() < 0.2F;
            GenericFx fx;
            if (absorb) {
                fx = new GenericFx(level, x, y, z, 0.0, 0.0, 0.0).loop(true).gravity(sparkle ? 0.0F : 0.125F)
                        .slowDown(0.995);
            } else {
                fx = new GenericFx(level, x + random.nextFloat() - 0.5, y, z - random.nextFloat() + 0.5, 0.0, 0.0, 0.0)
                        .gravity(-0.1F).slowDown(0.95);
            }
            int age = 30 + random.nextInt(20);
            add(fx.lifetime(age).colour(AURA_RED, AURA_GREEN, AURA_BLUE).alphas(flicker(random, age))
                    .frames(sparkle ? GenericFx.SPARKLE_FRAMES : GenericFx.WISP_FRAMES, 16, 1).scales(0.5F, 0.125F)
                    .randomMovement(0.0025F, 0.001F, 0.0025F).delay(2 + random.nextInt(3)));
        }
    }

    /**
     * FXFluxExplosion: three bursts of a hundred and eighty motes flung out in every direction, purple fading to a
     * random colour each.
     */
    public static void fluxExplosion(ClientLevel level, double x, double y, double z) {
        RandomSource random = level.random;
        for (int i = 0; i < 3 * 180; i++) {
            boolean sparkle = random.nextFloat() < 0.2F;
            int age = 30 + random.nextInt(20);
            add(new GenericFx(level, x, y, z, sign(random), sign(random), sign(random))
                    .colours(1.0F, 0.0F, 1.0F, random.nextFloat(), random.nextFloat(), random.nextFloat())
                    .loop(true).slowDown(0.955).lifetime(age).alphas(flicker(random, age))
                    .frames(sparkle ? GenericFx.SPARKLE_FRAMES : GenericFx.WISP_FRAMES, 16, 1).scales(0.5F, 0.125F)
                    .randomMovement(0.0025F, 0.001F, 0.0025F).delay(2 + random.nextInt(3)));
        }
    }

    /**
     * One puff of the plume a matrix gives off while its vat vents flux: a slow pink cloud that swells from twice to
     * five times its size, fades out, turns, and drifts with the moon-phase wind.
     */
    public static void matrixPlume(ClientLevel level, BlockPos matrix) {
        RandomSource random = level.random;
        double x = matrix.getX() + 0.2 + random.nextFloat() * 0.6;
        double y = matrix.getY() + 0.2 + random.nextFloat() * 0.6;
        double z = matrix.getZ() + 0.2 + random.nextFloat() * 0.6;
        add(new GenericFx(level, x, y, z, 0.0, 0.0, 0.0).lifetime(100 + random.nextInt(60)).colour(1.0F, 0.3F, 0.9F)
                .alphas(0.5F, 0.0F).frames(CLOUD_FRAME, 1, 1).scales(2.0F, 5.0F).translucent().slowDown(1.0)
                .wind(0.001).spin(random.nextFloat(), random.nextBoolean() ? -1.0F : 1.0F));
    }

    /** TC6's alpha flicker: full at birth, random in between, gone at the end. */
    private static float[] flicker(RandomSource random, int age) {
        float[] alphas = new float[6 + random.nextInt(age / 3)];
        for (int a = 1; a < alphas.length - 1; a++) {
            alphas[a] = random.nextFloat();
        }
        alphas[0] = 1.0F;
        return alphas;
    }

    private static double sign(RandomSource random) {
        return (random.nextBoolean() ? random.nextFloat() : -random.nextFloat()) / 1.5F;
    }

    private static void add(GenericFx fx) {
        Minecraft.getInstance().particleEngine.add(fx);
    }
}
