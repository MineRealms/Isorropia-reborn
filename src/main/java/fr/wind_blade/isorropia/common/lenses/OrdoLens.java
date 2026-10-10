package fr.wind_blade.isorropia.common.lenses;

import fr.wind_blade.isorropia.common.compat.ScanBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Scholar's lens: whatever the wearer looks at is studied, exactly as if they had held a thaumometer up to it.
 * The scan itself is Thaumcraft's; the lens only decides when to ask for it, and the client draws the reading.
 */
public class OrdoLens extends Lens {
    /** Scanning is not free: one look every this many ticks, as upstream paced it. */
    private static final int INTERVAL = 10;

    public OrdoLens() {
        super(id("ordo_lens"), 0xD5D4EC);
    }

    @Override
    public void tick(ServerPlayer player, ItemStack goggles, Slot slot) {
        if (player.tickCount % INTERVAL != 0) {
            return;
        }
        ScanBridge.scanLookedAt(player);
    }
}
