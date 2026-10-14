package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.infusion.InfusionDisplayData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Sync both vat registries on login and after a data-pack reload, including script-generated recipes. */
@Mod.EventBusSubscriber(modid = Isorropia.MODID)
public final class InfusionRecipeSync {
    private InfusionRecipeSync() {
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        List<ServerPlayer> players = event.getPlayer() == null
                ? event.getPlayerList().getPlayers() : List.of(event.getPlayer());
        List<InfusionDisplayData.Entry> entries = InfusionDisplayData.serverEntries();
        for (ServerPlayer player : players) {
            if (entries.isEmpty()) send(player, new SyncInfusionRecipesPacket(true, true, List.of()));
            for (int i = 0; i < entries.size(); i += 4) {
                int end = Math.min(entries.size(), i + 4);
                send(player, new SyncInfusionRecipesPacket(i == 0, end == entries.size(), entries.subList(i, end)));
            }
        }
    }

    private static void send(ServerPlayer player, SyncInfusionRecipesPacket packet) {
        //? if forge {
        IsorropiaNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
        //?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        *///?}
    }
}
