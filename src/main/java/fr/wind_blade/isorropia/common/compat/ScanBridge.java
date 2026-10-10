package fr.wind_blade.isorropia.common.compat;

import dev.tc4port.thaumcraft.common.ScanManager;
import dev.tc4port.thaumcraft.research.ScanTarget;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one place the mod asks Thaumcraft to scan something. Thaumcraft's public scan API only answers whether a
 * stack has been scanned; studying what the player looks at is {@code common.ScanManager}, which the thaumometer
 * itself uses. Kept here as a version checkpoint: an upgrade that moves this class must fail loudly here.
 */
public final class ScanBridge {
    private ScanBridge() {
    }

    /** Studies whatever the player is looking at, if it can be studied and has not been already. */
    public static boolean scanLookedAt(ServerPlayer player) {
        ScanTarget target = ScanManager.findTarget(player);
        if (target == null || !ScanManager.isValidTarget(player, target)
                || ScanManager.hasBeenScanned(player, target)) {
            return false;
        }
        return ScanManager.complete(player, target);
    }

    /** True if the player has already studied what they are looking at. */
    public static boolean alreadyScanned(ServerPlayer player) {
        ScanTarget target = ScanManager.findTarget(player);
        return target != null && ScanManager.hasBeenScanned(player, target);
    }
}
