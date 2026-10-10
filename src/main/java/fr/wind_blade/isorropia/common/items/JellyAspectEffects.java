package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.common.IsorropiaAspects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Per-aspect jelly food behaviour; aspects without an entry use {@link #DEFAULT}. */
public final class JellyAspectEffects {
    public static final IJellyAspectEffectProvider DEFAULT = new IJellyAspectEffectProvider() {
    };
    private static final Map<AspectId, IJellyAspectEffectProvider> EFFECTS = new ConcurrentHashMap<>();

    static {
        register(IsorropiaAspects.MOTUS, new IJellyAspectEffectProvider() {
            @Override
            public void onEaten(LivingEntity eater, ItemStack stack) {
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2, false, false));
            }
        });
        register(IsorropiaAspects.VICTUS, new IJellyAspectEffectProvider() {
            @Override
            public void onEaten(LivingEntity eater, ItemStack stack) {
                eater.heal(3.0F);
            }
        });
        register(IsorropiaAspects.PERMUTATIO, new IJellyAspectEffectProvider() {
            @Override
            public void onEaten(LivingEntity eater, ItemStack stack) {
                eater.hurt(eater.damageSources().magic(), 3.0F);
            }

            @Override
            public int getNutrition(ItemStack stack) {
                return 7;
            }
        });
        register(IsorropiaAspects.TUTAMEN, new IJellyAspectEffectProvider() {
            @Override
            public void onEaten(LivingEntity eater, ItemStack stack) {
                eater.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2, false, false));
            }
        });
    }

    private JellyAspectEffects() {
    }

    public static void register(AspectId aspect, IJellyAspectEffectProvider provider) {
        EFFECTS.put(aspect, provider);
    }

    public static IJellyAspectEffectProvider get(@Nullable AspectId aspect) {
        return aspect == null ? DEFAULT : EFFECTS.getOrDefault(aspect, DEFAULT);
    }
}
