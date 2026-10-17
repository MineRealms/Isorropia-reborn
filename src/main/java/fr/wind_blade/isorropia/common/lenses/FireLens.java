package fr.wind_blade.isorropia.common.lenses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Heinrich's lens: the wearer sees in the dark for as long as it is fitted, and loses that sight when it comes
 * out. Upstream applied the effect on the client every tick and dropped it on removal; here it is the same
 * bargain, applied on the server so it survives a relog.
 *
 * <p>Thaumic Horizons' own weakness is kept: the lens is fire, so it stops working while the wearer's eyes are
 * underwater. Its effect is also recognised by its short duration, so taking the lens out never cancels a night
 * vision potion the wearer drank.</p>
 */
public class FireLens extends Lens {
    /** A little under the effect's full length, so it is refreshed before it can lapse. */
    private static final int DURATION = 255;
    private static final int REFRESH_BELOW = 242;

    public FireLens() {
        super(id("fire_lens"), 0xFF7E2E);
    }

    @Override
    public void tick(ServerPlayer player, ItemStack goggles, Slot slot) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean granted = grantedByLens(current);
        // Upstream's one weakness: fire cannot work beneath the waves.
        if (player.isUnderWater()) {
            if (granted) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            return;
        }
        if (current == null || (granted && current.getDuration() < REFRESH_BELOW)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION, 0, false, false));
        }
    }

    @Override
    public void onRemoved(ServerPlayer player) {
        // Only take back what this lens gave: a potion's night vision outlives the lens.
        if (grantedByLens(player.getEffect(MobEffects.NIGHT_VISION))) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /**
     * The lens refreshes its effect at 242 of 255 ticks, so anything it granted is short; a potion's night vision
     * is minutes long. That difference is the marker upstream kept on a custom PotionEffect subclass.
     */
    private static boolean grantedByLens(@Nullable MobEffectInstance effect) {
        return effect != null && effect.getDuration() <= DURATION;
    }
}
