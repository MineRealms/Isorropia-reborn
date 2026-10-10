package fr.wind_blade.isorropia.client.fx;

import dev.tc4port.thaumcraft.api.client.ThaumcraftClientEffectsApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only creature effects. Callers must only invoke these on the logical client. */
public final class CreatureFx {
    private static final int GRAVEKEEPER_BEAM = 0xFFEE44;

    private CreatureFx() {
    }

    /** The gravekeeper's holy beam to an undead it is burning (TC6 used a bore beam; TC4R offers essentia trails). */
    public static void gravekeeperBeam(Entity source, Entity target) {
        if (!(source.level() instanceof ClientLevel level)) {
            return;
        }
        Vec3 from = source.position().add(0.0D, source.getBbHeight() / 2.0D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
        ThaumcraftClientEffectsApi.spawnEssentiaTrail(level, from, to, source.tickCount, GRAVEKEEPER_BEAM, 1.0F);
    }
}
