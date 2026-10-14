package fr.wind_blade.isorropia.common.network;

import fr.wind_blade.isorropia.common.lenses.Lens;
import fr.wind_blade.isorropia.common.lenses.LensManager;
import fr.wind_blade.isorropia.common.lenses.Lenses;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.network.NetworkEvent;
//?}

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Fitting or removing a lens. The client only chooses a slot, an inventory slot and which lens; the server
 * decides whether the player is wearing goggles, whether they have that lens, and what to do about it.
 */
//? if forge {
public record LensChangePacket(Lens.Slot slot, int inventorySlot, @Nullable ResourceLocation lens, boolean remove) {
//?} else {
/*public record LensChangePacket(Lens.Slot slot, int inventorySlot, @Nullable ResourceLocation lens, boolean remove) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<LensChangePacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(fr.wind_blade.isorropia.Isorropia.id("lens_change"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, LensChangePacket> CODEC =
            net.minecraft.network.codec.StreamCodec.ofMember(LensChangePacket::encode, LensChangePacket::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<LensChangePacket> type() {
        return TYPE;
    }
*///?}

    /** Takes the lens out of the given slot. */
    public static LensChangePacket removing(Lens.Slot slot) {
        return new LensChangePacket(slot, -1, null, true);
    }

    /** Fits the lens held in the given inventory slot. */
    public static LensChangePacket fitting(Lens.Slot slot, int inventorySlot, ResourceLocation lens) {
        return new LensChangePacket(slot, inventorySlot, lens, false);
    }

    public static void encode(LensChangePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.slot);
        buf.writeVarInt(packet.inventorySlot);
        buf.writeBoolean(packet.lens != null);
        if (packet.lens != null) {
            buf.writeResourceLocation(packet.lens);
        }
        buf.writeBoolean(packet.remove);
    }

    public static LensChangePacket decode(FriendlyByteBuf buf) {
        Lens.Slot slot = buf.readEnum(Lens.Slot.class);
        int inventorySlot = buf.readVarInt();
        ResourceLocation lens = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new LensChangePacket(slot, inventorySlot, lens, buf.readBoolean());
    }

    //? if forge {
    public static void handle(LensChangePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            apply(packet, player);
        });
    }
    //?} else {
    /*public static void handle(LensChangePacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                apply(packet, player);
            }
        });
    }
    *///?}

    private static void apply(LensChangePacket packet, ServerPlayer player) {
        if (packet.remove) {
            LensManager.remove(player, packet.slot);
            return;
        }
        Lens lens = Lenses.byId(packet.lens);
        if (lens == null || packet.inventorySlot < 0
                || packet.inventorySlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack held = player.getInventory().getItem(packet.inventorySlot);
        if (LensManager.itemFor(lens).is(held.getItem())) {
            LensManager.fit(player, packet.slot, lens, held);
        }
    }
}
