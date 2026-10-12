package fr.wind_blade.isorropia.common.infusion;

import fr.wind_blade.isorropia.common.capabilities.LivingData;
import fr.wind_blade.isorropia.common.compat.ThaumBridge;
import fr.wind_blade.isorropia.common.tiles.TileSoulBeacon;
import fr.wind_blade.isorropia.common.tiles.TileVat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;

/**
 * The reincarnation ritual. A wizard bound to a soul beacon who is dealt a killing blow is not killed: they wake
 * in the body waiting in the vat below the beacon, with that body's abilities, and climb out of the vat when
 * ready. Everything the old body carried is left behind, which is upstream's bargain — a new body costs the old
 * one's possessions. The beacon is single use.
 */
public final class SoulBeacon {
    private SoulBeacon() {
    }

    /**
     * Handles a killing blow on a bound player, if the beacon and a ready body are where they were left. True
     * means the blow became a revival and the caller must not run the ordinary death penalty.
     */
    public static boolean tryRevive(LivingHurtEvent event, ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(TileSoulBeacon.BOUND)) {
            return false;
        }
        int[] coords = data.getIntArray(TileSoulBeacon.COORDS);
        if (coords.length != 3) {
            return false;
        }
        ServerLevel level = beaconLevel(player, data.getString(TileSoulBeacon.DIMENSION));
        if (level == null) {
            return false;
        }
        BlockPos beaconPos = new BlockPos(coords[0], coords[1], coords[2]);
        if (!(level.getBlockEntity(beaconPos) instanceof TileSoulBeacon)) {
            return false;
        }
        // The body waits directly below the beacon, at the vat's master (top centre).
        if (!(level.getBlockEntity(beaconPos.below()) instanceof TileVat vat)) {
            return false;
        }
        TileVat master = vat.master();
        if (master == null || master.getMode() != TileVat.MODE_SELF_READY) {
            return false;
        }
        event.setCanceled(true);
        revive(player, level, beaconPos, master);
        return true;
    }

    /** The level the beacon was bound in, matched by dimension name, or null if that level is gone. */
    @Nullable
    private static ServerLevel beaconLevel(ServerPlayer player, String dimension) {
        if (dimension.isEmpty()) {
            return null;
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    private static void revive(ServerPlayer player, ServerLevel level, BlockPos beaconPos, TileVat vat) {
        // The spent body tears itself apart where it fell.
        level.explode(null, player.getX(), player.getEyeY(), player.getZ(), 0.0F, Level.ExplosionInteraction.NONE);
        for (int i = 0; i < 25; i++) {
            BlockPos spill = player.blockPosition().offset(level.random.nextInt(17) - 8,
                    level.random.nextInt(17) - 8, level.random.nextInt(17) - 8);
            ThaumBridge.releaseFlux(level, spill, 8);
        }
        // Everything the old body carried stays behind, baubles included.
        player.getInventory().dropAll();
        dropCurios(player);
        player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
        player.heal(999.0F);
        // The new body is the one in the vat: its abilities pass to the player and the vat is emptied.
        player.teleportTo(level, beaconPos.getX() + 0.5, beaconPos.getY() - 2.5, beaconPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        vat.claimSelfInfusions(player);
        // The wizard wakes inside the vat, as upstream had it, and leaves it the way any occupant does.
        vat.setContained(player);
        player.getPersistentData().putBoolean(TileSoulBeacon.BOUND, false);
        LivingData.get(player).ifPresent(LivingData::sync);
    }

    /** Ejects every worn curio where the player stands, the way the main inventory is dropped. */
    private static void dropCurios(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(handler -> {
            IItemHandlerModifiable worn = handler.getEquippedCurios();
            for (int slot = 0; slot < worn.getSlots(); slot++) {
                ItemStack stack = worn.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    player.drop(stack.copy(), false);
                    worn.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        });
    }
}
