package fr.wind_blade.isorropia.common.network;

import com.google.gson.JsonParser;
import fr.wind_blade.isorropia.common.infusion.InfusionDisplayData;
import net.minecraft.network.FriendlyByteBuf;
//? if forge {
import net.minecraftforge.network.NetworkEvent;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Small batches keep data-pack recipe snapshots below the clientbound payload limit. */
//? if forge {
public record SyncInfusionRecipesPacket(boolean reset, boolean last, List<InfusionDisplayData.Entry> entries) {
//?} else {
/*public record SyncInfusionRecipesPacket(boolean reset, boolean last, List<InfusionDisplayData.Entry> entries)
        implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SyncInfusionRecipesPacket> TYPE = new Type<>(fr.wind_blade.isorropia.Isorropia.id("infusion_recipes"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SyncInfusionRecipesPacket> CODEC =
            net.minecraft.network.codec.StreamCodec.ofMember(SyncInfusionRecipesPacket::encode, SyncInfusionRecipesPacket::decode);

    @Override
    public Type<SyncInfusionRecipesPacket> type() {
        return TYPE;
    }
*///?}
    private static final List<InfusionDisplayData.Entry> PENDING = new ArrayList<>();

    public static void encode(SyncInfusionRecipesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.reset);
        buffer.writeBoolean(packet.last);
        buffer.writeVarInt(packet.entries.size());
        for (var entry : packet.entries) {
            buffer.writeResourceLocation(entry.id());
            buffer.writeBoolean(entry.self());
            buffer.writeUtf(entry.json().toString());
        }
    }

    public static SyncInfusionRecipesPacket decode(FriendlyByteBuf buffer) {
        boolean reset = buffer.readBoolean();
        boolean last = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > 4) throw new IllegalArgumentException("Invalid infusion batch size: " + count);
        List<InfusionDisplayData.Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new InfusionDisplayData.Entry(buffer.readResourceLocation(), buffer.readBoolean(),
                    JsonParser.parseString(buffer.readUtf()).getAsJsonObject()));
        }
        return new SyncInfusionRecipesPacket(reset, last, List.copyOf(entries));
    }

    //? if forge {
    public static void handle(SyncInfusionRecipesPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> accept(packet));
    }
    //?} else {
    /*public static void handle(SyncInfusionRecipesPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> accept(packet));
    }
    *///?}

    private static void accept(SyncInfusionRecipesPacket packet) {
        if (packet.reset) PENDING.clear();
        PENDING.addAll(packet.entries);
        if (packet.last) {
            InfusionDisplayData.accept(PENDING);
            PENDING.clear();
        }
    }
}
