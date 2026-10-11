package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.common.capabilities.LivingData;
import fr.wind_blade.isorropia.common.infusion.SelfInfusions;
import fr.wind_blade.isorropia.common.menu.MorphicFingersMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Asks the server to open the morphic fingers' workbench. The client sends it when the key is pressed; the
 * server decides, because only the server can be trusted about whether the fingers were grown.
 */
public record OpenFingersPacket() {
    public static void encode(OpenFingersPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenFingersPacket decode(FriendlyByteBuf buf) {
        return new OpenFingersPacket();
    }

    public static void handle(OpenFingersPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            LivingData data = LivingData.get(player).orElse(null);
            if (data == null || !data.hasSelfInfusion(SelfInfusions.MORPHIC_FINGERS)) {
                return;
            }
            player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new MorphicFingersMenu(id, inventory),
                    Component.translatable("container.isorropia.morphic_fingers")));
        });
    }
}
