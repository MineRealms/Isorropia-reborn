package fr.wind_blade.isorropia.common.lenses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Kirlian lens: shows the life and death in living things as ripples and vortices in the world. Nothing happens
 * on the server — the wearer's client draws it from the entities around them.
 */
public class AirLens extends Lens {
    public AirLens() {
        super(id("air_lens"), 0x9FC3E8);
    }

    @Override
    public void tick(ServerPlayer player, ItemStack goggles, Slot slot) {
        // Client-side effect only.
    }
}
