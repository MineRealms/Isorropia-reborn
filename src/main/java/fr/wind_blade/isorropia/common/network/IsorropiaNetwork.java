package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The single mod channel. Packets register themselves through {@link #register} from {@link #init};
 * ids are assigned in call order, so append new packets at the end of {@link #init}.
 */
public final class IsorropiaNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Isorropia.id("main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    private IsorropiaNetwork() {
    }

    public static void init() {
        register(SyncLivingDataPacket.class, NetworkDirection.PLAY_TO_CLIENT,
                SyncLivingDataPacket::encode, SyncLivingDataPacket::decode, SyncLivingDataPacket::handle);
        register(ClientFxPacket.class, NetworkDirection.PLAY_TO_CLIENT,
                ClientFxPacket::encode, ClientFxPacket::decode, ClientFxPacket::handle);
        register(LensChangePacket.class, NetworkDirection.PLAY_TO_SERVER,
                LensChangePacket::encode, LensChangePacket::decode, LensChangePacket::handle);
    }

    public static <M> void register(Class<M> type, NetworkDirection direction, BiConsumer<M, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, (message, context) -> {
            handler.accept(message, context);
            context.get().setPacketHandled(true);
        }, Optional.of(direction));
    }
}
