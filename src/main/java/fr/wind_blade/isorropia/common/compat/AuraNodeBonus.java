package fr.wind_blade.isorropia.common.compat;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Call site for TH's transduction-amplifier boost. TC4R has no public API to raise an energized node's
 * base vis, so {@link AuraNodeBlockEntityMixin} adds the method and we bridge to it through reflection.
 * If the mixin is disabled the calls become no-ops instead of crashing.
 */
public final class AuraNodeBonus {
    private static final Method ADD;
    private static final Method GET;

    static {
        Method a = null;
        Method g = null;
        try {
            a = AuraNodeBlockEntity.class.getMethod("isorropia$addTransducerBonus", AspectId.class, int.class);
            g = AuraNodeBlockEntity.class.getMethod("isorropia$transducerBonus");
        } catch (ReflectiveOperationException ignored) {
            // Mixin not applied (required: false): bonuses are simply unavailable.
        }
        ADD = a;
        GET = g;
    }

    public static void add(AuraNodeBlockEntity node, AspectId aspect, int delta) {
        if (ADD != null) {
            try {
                ADD.invoke(node, aspect, delta);
            } catch (ReflectiveOperationException ignored) {
                // best-effort; a failed boost should not break the transducer tick
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<AspectId, Integer> get(AuraNodeBlockEntity node) {
        if (GET != null) {
            try {
                return (Map<AspectId, Integer>) GET.invoke(node);
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        return Map.of();
    }

    private AuraNodeBonus() {
    }
}
