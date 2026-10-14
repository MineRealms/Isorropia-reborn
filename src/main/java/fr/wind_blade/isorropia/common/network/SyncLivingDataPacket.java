package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.common.capabilities.LivingData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
//? if forge {
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
//?}

import java.util.function.Supplier;

//? if forge {
public record SyncLivingDataPacket(int entityId, CompoundTag data) {
//?} else {
/*public record SyncLivingDataPacket(int entityId, CompoundTag data) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncLivingDataPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(fr.wind_blade.isorropia.Isorropia.id("sync_living_data"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SyncLivingDataPacket> CODEC =
            net.minecraft.network.codec.StreamCodec.ofMember(SyncLivingDataPacket::encode, SyncLivingDataPacket::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncLivingDataPacket> type() {
        return TYPE;
    }
*///?}
    public static void encode(SyncLivingDataPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeNbt(packet.data);
    }

    public static SyncLivingDataPacket decode(FriendlyByteBuf buf) {
        return new SyncLivingDataPacket(buf.readVarInt(), buf.readNbt());
    }

    //? if forge {
    public static void handle(SyncLivingDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(packet)));
    }
    //?} else {
    /*public static void handle(SyncLivingDataPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                Client.apply(packet);
            }
        });
    }
    *///?}

    private static final class Client {
        static void apply(SyncLivingDataPacket packet) {
            if (Minecraft.getInstance().level == null || packet.data == null) {
                return;
            }
            Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId);
            if (entity instanceof LivingEntity living) {
                LivingData.get(living).ifPresent(data -> data.deserialize(packet.data));
            }
        }
    }
}
