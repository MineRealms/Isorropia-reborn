package fr.wind_blade.isorropia.mixin;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backs TH's transduction amplifier. TC4R derives an energized node's base vis from its structure
 * signature and recomputes it on every energized tick, so there is no public setter for a persistent
 * bonus. We keep our own per-node {@code transducerBonus} and fold it into
 * {@code calculateEnergizedBase} (which both the public getter and the tick use), so the boost survives
 * the structure recheck. The live caches are refreshed on the next energized tick because
 * {@code availableVis} is rebuilt from {@code energizedBase} each tick.
 *
 * <p>The bonus is intentionally not persisted on the node: the owning transducer re-applies it from its
 * own saved state on load, so the node only ever reflects whatever amplifiers are currently attached.</p>
 */
@Mixin(targets = "dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity", remap = false)
public abstract class AuraNodeBlockEntityMixin {
    @Shadow
    private Map<AspectId, Integer> energizedBase;

    @Unique
    private final Map<AspectId, Integer> isorropia$transducerBonus = new LinkedHashMap<>();

    /** Add (or, with a negative delta, remove) a per-aspect bonus to this node's energized base vis. */
    @Unique
    public void isorropia$addTransducerBonus(AspectId aspect, int delta) {
        int next = isorropia$transducerBonus.getOrDefault(aspect, 0) + delta;
        if (next <= 0) {
            isorropia$transducerBonus.remove(aspect);
        } else {
            isorropia$transducerBonus.put(aspect, next);
        }
        // Force a recompute on the next energized tick; calculateEnergizedBase folds the bonus in.
        if (!this.energizedBase.isEmpty()) {
            this.energizedBase = Map.of();
        }
    }

    @Unique
    public Map<AspectId, Integer> isorropia$transducerBonus() {
        return new LinkedHashMap<>(isorropia$transducerBonus);
    }

    @Inject(method = "calculateEnergizedBase(Ljava/util/Map;)Ljava/util/Map;", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void isorropia$foldBonus(Map<AspectId, Integer> reducedInput,
            CallbackInfoReturnable<Map<AspectId, Integer>> cir) {
        if (isorropia$transducerBonus.isEmpty()) {
            return;
        }
        Map<AspectId, Integer> merged = new LinkedHashMap<>(cir.getReturnValue());
        for (Map.Entry<AspectId, Integer> entry : isorropia$transducerBonus.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        cir.setReturnValue(merged);
    }
}
