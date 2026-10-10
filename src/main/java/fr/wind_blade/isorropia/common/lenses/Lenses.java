package fr.wind_blade.isorropia.common.lenses;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The four lenses Isorropia makes. Upstream had a Forge registry for these; the registry API is gone in 1.20.1,
 * and a lens is just behaviour attached to an item, so they are kept by id here.
 */
public final class Lenses {
    private static final Map<ResourceLocation, Lens> LENSES = new LinkedHashMap<>();

    public static final AirLens AIR = register(new AirLens());
    public static final FireLens FIRE = register(new FireLens());
    public static final OrdoLens ORDO = register(new OrdoLens());
    public static final EnvyLens ENVY = register(new EnvyLens());

    private Lenses() {
    }

    private static <T extends Lens> T register(T lens) {
        LENSES.put(lens.id(), lens);
        return lens;
    }

    @Nullable
    public static Lens byId(@Nullable ResourceLocation id) {
        return id == null ? null : LENSES.get(id);
    }

    public static Map<ResourceLocation, Lens> all() {
        return LENSES;
    }
}
