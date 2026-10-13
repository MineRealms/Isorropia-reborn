package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thaumic Horizons' {@code PocketPlaneData.addEffects}: a pocket plane grants the visitor a set of potion effects
 * derived from the aspects the plane was made of. TH walked a fixed list of resonant aspects and appended one potion id
 * per match into an {@code int[8]} stored on the plane (written and read back through {@code PocketPlaneData}'s NBT).
 * This port keeps the order and the derivation but returns the modern {@link MobEffect}s directly.
 *
 * <p><b>Faithful detail:</b> TH did not de-duplicate. Both {@code motus} (MOTION) and {@code iter} (TRAVEL) map to the
 * speed effect, so a plane holding both lists it twice. That is kept, because the array TH stored did too.</p>
 *
 * <p>TH never actually applied the stored ids anywhere - the array is only persisted - so this derivation is the whole
 * of the behaviour. A caller that wants to hand the effects out can apply these entries with whatever duration and
 * amplifier it likes.</p>
 */
public final class PocketPlaneEffects {
    private PocketPlaneEffects() {
    }

    /**
     * TH's {@code addEffects}, in TH's aspect order: MOTION, FLIGHT, HEAL, TRAVEL, TOOL, WEAPON, ARMOR. The result is
     * capped at the eight slots TH reserved.
     */
    public static List<MobEffect> forAspects(Map<AspectId, Integer> aspects) {
        List<MobEffect> effects = new ArrayList<>(8);
        if (amount(aspects, "motus") > 0) {
            effects.add(MobEffects.MOVEMENT_SPEED);
        }
        if (amount(aspects, "volatus") > 0) {
            effects.add(MobEffects.JUMP);
        }
        if (amount(aspects, "sano") > 0) {
            effects.add(MobEffects.REGENERATION);
        }
        if (amount(aspects, "iter") > 0) {
            effects.add(MobEffects.MOVEMENT_SPEED);
        }
        if (amount(aspects, "instrumentum") > 0) {
            effects.add(MobEffects.DIG_SPEED);
        }
        if (amount(aspects, "telum") > 0) {
            effects.add(MobEffects.DAMAGE_BOOST);
        }
        if (amount(aspects, "tutamen") > 0) {
            effects.add(MobEffects.DAMAGE_RESISTANCE);
        }
        return effects;
    }

    private static int amount(Map<AspectId, Integer> aspects, String path) {
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            if (entry.getKey().value().getPath().equals(path)) {
                return Math.max(0, entry.getValue());
            }
        }
        return 0;
    }
}
