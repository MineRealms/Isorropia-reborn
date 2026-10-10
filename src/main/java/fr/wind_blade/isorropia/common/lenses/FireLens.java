package fr.wind_blade.isorropia.common.lenses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * Heinrich's lens: the wearer sees in the dark for as long as it is fitted, and loses that sight when it comes
 * out. Upstream applied the effect on the client every tick and dropped it on removal; here it is the same
 * bargain, applied on the server so it survives a relog.
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
        if (current == null || current.getDuration() < REFRESH_BELOW) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION, 0, false, false));
        }
    }

    @Override
    public void onRemoved(ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
