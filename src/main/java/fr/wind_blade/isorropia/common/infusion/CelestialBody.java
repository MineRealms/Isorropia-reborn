package fr.wind_blade.isorropia.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Sky bodies a creature infusion can require. Their "celestial aura" is gathered one point per vat cycle
 * while the body can be drained: the vat must see the sky in the overworld, and the body must be up.
 */
public enum CelestialBody {
    NONE(-2),
    SUN(-2),
    STARS(-2),
    MOON(-1),
    MOON_FULL(0),
    MOON_WANING_GIBBOUS(1),
    MOON_THIRD_QUARTER(2),
    MOON_WANING_CRESCENT(3),
    MOON_NEW(4),
    MOON_WAXING_CRESCENT(5),
    MOON_FIRST_QUARTER(6);

    /** -2 for non-moon bodies, -1 for "any phase", else the vanilla moon phase index. */
    private final int moonPhase;

    CelestialBody(int moonPhase) {
        this.moonPhase = moonPhase;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CelestialBody byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }

    public boolean canBeDrained(Level level, BlockPos skyCheck) {
        if (this == NONE || level.dimension() != Level.OVERWORLD || !level.canSeeSky(skyCheck)) {
            return false;
        }
        return switch (this) {
            case SUN -> isDay(level);
            case STARS -> !isDay(level);
            default -> !level.isRaining() && !isDay(level) && (moonPhase < 0 || level.getMoonPhase() == moonPhase);
        };
    }

    /** True for a specific phase sharing gathered aura with the generic moon. */
    public boolean sharesAuraWith(CelestialBody other) {
        return this == other || (moonPhase >= 0 && other == MOON);
    }

    private static boolean isDay(Level level) {
        float angle = level.getTimeOfDay(1.0F);
        return (angle + 0.25F) * 360.0F % 360.0F <= 180.0F;
    }
}
