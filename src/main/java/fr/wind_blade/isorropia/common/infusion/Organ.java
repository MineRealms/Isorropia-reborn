package fr.wind_blade.isorropia.common.infusion;

import java.util.Locale;

/** A creature holds at most one organ infusion per organ; a new one replaces (and undoes) the old. */
public enum Organ {
    HEART, SKIN, BLOOD, MUSCLE, VOID;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Organ byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
