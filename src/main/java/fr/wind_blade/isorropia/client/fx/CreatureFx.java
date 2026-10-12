package fr.wind_blade.isorropia.client.fx;

import dev.tc4port.thaumcraft.api.client.BeamSettings;
import dev.tc4port.thaumcraft.api.client.BeamTexture;
import dev.tc4port.thaumcraft.api.client.EffectColor;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientEffectsApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only creature effects. Callers must only invoke these on the logical client. */
public final class CreatureFx {
    /**
     * Thaumic Horizons' gravekeeper beam: Thaumcraft 4's beam of type 0 in 0xFFEE44, not reversed, widening to 2.5
     * at the far end. The rest is TC4's beam as it always drew: eight ticks of life, pulsing, added to the scene.
     */
    private static final BeamSettings GRAVEKEEPER_BEAM = new BeamSettings(BeamTexture.DEFAULT, EffectColor.rgb(0xFFEE44),
            8, 1.0F, 2.5F, false, true, 5, true);

    private CreatureFx() {
    }

    /** The gravekeeper's holy beam to an undead it is burning, from its middle to the target, which it follows. */
    public static void gravekeeperBeam(Entity source, Entity target) {
        if (!(source.level() instanceof ClientLevel level)) {
            return;
        }
        Vec3 from = source.position().add(0.0D, source.getBbHeight() / 2.0D, 0.0D);
        ThaumcraftClientEffectsApi.spawnBeam(level, from, target, GRAVEKEEPER_BEAM);
    }
}
