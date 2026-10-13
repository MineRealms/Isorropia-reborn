package fr.wind_blade.isorropia.common.menu;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.player.PlayerKnowledgeApi;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.items.misc.ItemSyringeBlood;
import fr.wind_blade.isorropia.common.tiles.TileBloodInfuser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * TH's {@code ContainerBloodInfuser}: one blood-syringe slot, a three-by-three output rack, and TH's button
 * protocol — negative ids add the sorted known aspect {@code (-id)-1}, ids above two remove {@code id-3},
 * and zero through two set the mode. TH resolved button ids against the opener's own discovered aspects, so
 * both ends re-sort {@link PlayerKnowledgeApi#view} the same way.
 */
public class BloodInfuserMenu extends AbstractContainerMenu {
    @Nullable
    private final TileBloodInfuser tile;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    /** Client side: the tile arrives through the level once the block entity syncs. */
    public BloodInfuserMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, buffer.readBlockPos());
    }

    public BloodInfuserMenu(int id, Inventory inventory, TileBloodInfuser tile) {
        this(id, inventory, tile, tile.getBlockPos());
    }

    private BloodInfuserMenu(int id, Inventory inventory, @Nullable TileBloodInfuser tile, BlockPos pos) {
        super(MenusIS.BLOOD_INFUSER.get(), id);
        this.tile = tile;
        this.pos = pos;
        this.access = tile == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos());
        // The client's slots are cosmetic; item stacks arrive as menu sync.
        Container slots = tile != null ? tile.container() : new net.minecraft.world.SimpleContainer(10) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof ItemSyringeBlood;
            }
        };
        this.addSlot(new Slot(slots, 0, 16, 37));
        for (int x = 0; x < 3; ++x) {
            for (int y = 0; y < 3; ++y) {
                this.addSlot(new Slot(slots, x * 3 + y + 1, 108 + x * 18, 19 + y * 18));
            }
        }
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 137 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 195));
        }
    }

    /** The opener's discovered aspects, sorted the same way on both ends (TH's {@code getAspectsSorted}). */
    public static List<AspectId> knownSorted(Player player) {
        return PlayerKnowledgeApi.view(player).discoveredAspects().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** The machine's position, for the screen to find the synced block entity. */
    public BlockPos pos() {
        return this.pos;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.tile == null) {
            return false;
        }
        List<AspectId> known = knownSorted(player);
        if (id < 0) {
            int index = -1 - id;
            if (index < known.size()) {
                this.tile.addSelected(known.get(index));
            }
            return true;
        }
        if (id > 2) {
            int index = id - 3;
            if (index < known.size()) {
                this.tile.removeSelected(known.get(index));
            }
            return true;
        }
        this.tile.setMode(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 10) {
            if (!this.moveItemStackTo(stack, 10, 46, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemSyringeBlood) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlocksIS.BLOOD_INFUSER.get());
    }
}
