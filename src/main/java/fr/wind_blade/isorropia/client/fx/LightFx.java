package fr.wind_blade.isorropia.client.fx;

import dev.tc4port.thaumcraft.api.client.ThaumcraftClientEffectsApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * TH's {@code ClientProxy.illuminationFX}: each tick a mote may throw off one weightless, unshrinking half-size
 * wisp in its colour, more often at higher particle settings.
 */
public final class LightFx {
    private static final float SIZE = 0.5F;

    private LightFx() {
    }

    public static void tick(Level level, BlockPos pos, int colour) {
        if (!(level instanceof ClientLevel client)
                || level.random.nextInt(9 - ThaumcraftClientEffectsApi.qualityScaledParticleCount(2)) != 0) {
            return;
        }
        // TH nudged red off zero, which TC4's wisp would otherwise have read as "no colour given".
        float red = Math.min(1.0F, (colour >> 16 & 255) / 255.0F + 0.01F);
        float green = (colour >> 8 & 255) / 255.0F;
        float blue = (colour & 255) / 255.0F;
        ThaumcraftClientEffectsApi.spawnWispMote(client, new Vec3(pos.getX() + 0.55, pos.getY() + 0.5, pos.getZ() + 0.55),
                SIZE, red, green, blue, false, 0.0F);
    }
}
