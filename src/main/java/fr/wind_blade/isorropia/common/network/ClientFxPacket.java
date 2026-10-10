package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.client.fx.IsorropiaFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * One effect for the client to play. Effects that only the server can know about — a pedestal item being taken,
 * an instability event firing — are broadcast with this, so the client does not have to guess them from its own
 * copy of the block entity's state.
 */
public record ClientFxPacket(Kind kind, Vec3 from, Vec3 to, float red, float green, float blue, int count,
        float strength) {

    public enum Kind {
        /** Runes on the floor of the vat while it infuses. */
        RUNE,
        /** Lightning between two points. */
        ARC,
        /** One ingredient flying from a pedestal into the vat. */
        ITEM_FLIGHT,
        /** The burst when a vat tears itself apart. */
        FLUX_BURST
    }

    public static void encode(ClientFxPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.kind);
        writeVec(buf, packet.from);
        writeVec(buf, packet.to);
        buf.writeFloat(packet.red);
        buf.writeFloat(packet.green);
        buf.writeFloat(packet.blue);
        buf.writeVarInt(packet.count);
        buf.writeFloat(packet.strength);
    }

    public static ClientFxPacket decode(FriendlyByteBuf buf) {
        return new ClientFxPacket(buf.readEnum(Kind.class), readVec(buf), readVec(buf), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readFloat());
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(ClientFxPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> IsorropiaFx.play(packet)));
    }

    /** Sends an effect to everyone able to see a point, within the usual tracking range. */
    public static void send(ServerLevel level, Vec3 at, ClientFxPacket packet) {
        IsorropiaNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(at.x, at.y,
                at.z, 64.0, level.dimension())), packet);
    }

    public static void rune(ServerLevel level, Vec3 at, int count) {
        send(level, at, new ClientFxPacket(Kind.RUNE, at, at, 0.55F, 0.1F, 1.0F, count, 0.0F));
    }

    public static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        send(level, from, new ClientFxPacket(Kind.ARC, from, to, 1.0F, 1.0F, 1.0F, 1, 0.0F));
    }

    public static void ingredientFlight(ServerLevel level, Vec3 from, Vec3 to) {
        send(level, to, new ClientFxPacket(Kind.ITEM_FLIGHT, from, to, 1.0F, 1.0F, 1.0F, 1, 0.0F));
    }

    public static void fluxBurst(ServerLevel level, Vec3 at) {
        send(level, at, new ClientFxPacket(Kind.FLUX_BURST, at, at, 1.0F, 1.0F, 1.0F, 1, 3.0F));
    }
}
