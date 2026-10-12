package fr.wind_blade.isorropia.common.menu;

import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.tiles.TileVisDynamo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * TH's {@code ContainerVisDynamo}: no slots, only the six primal switches. The screen's buttons arrive as
 * {@link #clickMenuButton} ids, which is the same vanilla button packet TH's {@code sendEnchantPacket} used.
 */
public class VisDynamoMenu extends AbstractContainerMenu {
    /** Button order, as TH numbered them from one: aer, terra, ignis, aqua, ordo, perditio. */
    public static final VisChannel[] BUTTONS = {
            VisChannel.AER, VisChannel.TERRA, VisChannel.IGNIS, VisChannel.AQUA, VisChannel.ORDO, VisChannel.PERDITIO
    };

    @Nullable
    private final TileVisDynamo dynamo;
    private final ContainerLevelAccess access;
    private final ContainerData switches;

    /** Client side: the switches arrive through the data slot. */
    public VisDynamoMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        super(MenusIS.VIS_DYNAMO.get(), id);
        buffer.readBlockPos();
        this.dynamo = null;
        this.access = ContainerLevelAccess.NULL;
        this.switches = new SimpleContainerData(1);
        addDataSlots(this.switches);
    }

    public VisDynamoMenu(int id, Inventory inventory, TileVisDynamo dynamo) {
        super(MenusIS.VIS_DYNAMO.get(), id);
        this.dynamo = dynamo;
        this.access = ContainerLevelAccess.create(dynamo.getLevel(), dynamo.getBlockPos());
        this.switches = dynamo.switchData();
        addDataSlots(this.switches);
    }

    public boolean enabled(VisChannel channel) {
        return (this.switches.get(0) & 1 << channel.index()) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= BUTTONS.length) {
            return false;
        }
        if (this.dynamo != null) {
            this.dynamo.toggle(BUTTONS[id]);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlocksIS.VIS_DYNAMO.get());
    }
}
