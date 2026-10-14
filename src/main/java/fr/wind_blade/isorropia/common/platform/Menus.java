package fr.wind_blade.isorropia.common.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** Opening a menu that carries its block position to the client screen. */
public final class Menus {
    private Menus() {
    }

    /** Forge needed its network hook for the extra data; NeoForge 1.21 folded it into the player. */
    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        //? if forge {
        net.minecraftforge.network.NetworkHooks.openScreen(player, provider, pos);
        //?} else {
        /*player.openMenu(provider, pos);
        *///?}
    }
}
