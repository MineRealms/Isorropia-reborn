package fr.wind_blade.isorropia.mixin;

import fr.wind_blade.isorropia.common.compat.HeldWandTargets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a machine own a wand held against it, as TC4's {@code IWandable.onUsingWandTick} did. Version-upgrade
 * checkpoint: TC4R's {@code WandItem.onUseTick} is named {@code onUseTick} in the dev environment and
 * {@code m_5929_} in a production jar, so both are listed and nothing is remapped.
 */
@Mixin(targets = "dev.tc4port.thaumcraft.item.WandItem", remap = false)
public abstract class WandItemMixin {
    @Inject(method = {"onUseTick", "m_5929_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void isorropia$machineHold(Level level, LivingEntity entity, ItemStack stack, int remaining,
            CallbackInfo callback) {
        if (HeldWandTargets.interceptUseTick(entity)) {
            callback.cancel();
        }
    }
}
