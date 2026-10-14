package fr.wind_blade.isorropia.common.menu;

import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectItemView;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.node.VisNetworkApi;
import dev.tc4port.thaumcraft.api.recipe.ArcaneCraftingApi;
import dev.tc4port.thaumcraft.api.recipe.ArcaneCraftingRecipe;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * The morphic fingers: an arcane workbench worn in the hands. It is the vat's self infusion, not a block, so
 * the menu is built from the player alone and everything in it is dropped when it closes — upstream kept
 * nothing either.
 *
 * <p>Vis is paid from the network the player stands in, exactly as the vat pays for its own work; the six
 * crystal slots hold vis crystals that make up whatever the network is short of, which is what those slots
 * held in the original.
 */
public class MorphicFingersMenu extends AbstractContainerMenu {
    private static final int GRID = 3;
    private static final int GRID_SLOTS = GRID * GRID;
    private static final int CRYSTALS = 6;
    /** One vis crystal pays one whole vis of its aspect. */
    private static final int CRYSTAL_VIS = 100;

    private static final int RESULT_SLOT = 0;
    private static final int GRID_START = 1;
    private static final int CRYSTAL_START = GRID_START + GRID_SLOTS;
    private static final int PLAYER_START = CRYSTAL_START + CRYSTALS;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int MENU_SIZE = HOTBAR_START + 9;

    /** The six crystal slots, laid out around the grid as the original's screen drew them. */
    private static final int[] CRYSTAL_X = {64, 17, 112, 17, 112, 64};
    private static final int[] CRYSTAL_Y = {13, 35, 35, 93, 93, 115};

    private final Inventory playerInventory;
    private final Player player;
    private final CraftingContainer craft;
    private final SimpleContainer crystals;
    private final ResultContainer result;

    /** Resolved on the server when the grid changes, and spent when the result is taken. */
    @Nullable
    private ArcaneCraftingRecipe arcane;
    @Nullable
    private CraftingRecipe vanilla;

    public MorphicFingersMenu(int containerId, Inventory playerInventory) {
        super(MenusIS.MORPHIC_FINGERS.get(), containerId);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.craft = new TransientCraftingContainer(this, GRID, GRID);
        this.crystals = new SimpleContainer(CRYSTALS);
        this.result = new ResultContainer();
        // The grid tells the menu it changed on its own; a plain container does not, and a crystal moving in
        // changes whether the recipe is payable at all.
        this.crystals.addListener(container -> updateResult());

        this.addSlot(new Slot(this.result, 0, 160, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return this.hasItem();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                MorphicFingersMenu.this.takeResult(player, stack);
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < GRID; row++) {
            for (int column = 0; column < GRID; column++) {
                this.addSlot(new Slot(this.craft, column + row * GRID, 40 + column * 24, 40 + row * 24));
            }
        }
        for (int i = 0; i < CRYSTALS; i++) {
            this.addSlot(new Slot(this.crystals, i, CRYSTAL_X[i], CRYSTAL_Y[i]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return crystalChannel(stack) != null;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 16 + column * 18, 209));
        }
        updateResult();
    }

    @Override
    public void slotsChanged(Container container) {
        updateResult();
    }

    /** The aspect a stack is a crystal of, or null for anything else. */
    @Nullable
    private static VisChannel crystalChannel(ItemStack stack) {
        if (!(stack.getItem() instanceof AspectItemView view)) {
            return null;
        }
        AspectAmounts aspects = view.storedAspectData(stack);
        if (aspects.amounts().size() != 1) {
            return null;
        }
        AspectId aspect = aspects.amounts().keySet().iterator().next();
        return VisChannel.fromAspectId(aspect).orElse(null);
    }

    /** Re-reads the grid: an arcane recipe if one is known and payable, otherwise a plain crafting one. */
    private void updateResult() {
        if (this.player.level().isClientSide) {
            // The result is the server's to decide and to sync; recomputing it here would only fight the packet.
            return;
        }
        this.arcane = null;
        this.vanilla = null;
        ItemStack out = ItemStack.EMPTY;
        Optional<ArcaneCraftingRecipe> arcane = findArcane();
        if (arcane.isPresent()) {
            this.arcane = arcane.get();
            if (canPay(this.arcane)) {
                out = this.arcane.assemble(input(), this.player.level().registryAccess());
            }
        } else {
            Optional<CraftingRecipe> plain = findVanilla();
            if (plain.isPresent()) {
                this.vanilla = plain.get();
                out = this.vanilla.assemble(input(), this.player.level().registryAccess());
            }
        }
        this.result.setItem(0, out);
        this.broadcastChanges();
    }

    private Optional<ArcaneCraftingRecipe> findArcane() {
        Level level = this.player.level();
        //? if forge {
        for (ArcaneCraftingRecipe recipe : ArcaneCraftingApi.recipes(level)) {
        //?} else {
        /*for (net.minecraft.world.item.crafting.RecipeHolder<ArcaneCraftingRecipe> holder : ArcaneCraftingApi.recipes(level)) {
            ArcaneCraftingRecipe recipe = holder.value();
        *///?}
            if (recipe.matches(input(), level) && knowsResearch(recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private boolean knowsResearch(ArcaneCraftingRecipe recipe) {
        return recipe.requiredResearch(input()).stream().allMatch(key -> ResearchApi.isComplete(this.player, key));
    }

    private Optional<CraftingRecipe> findVanilla() {
        return this.player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input(), this.player.level())
                //? if neoforge {
                /*.map(net.minecraft.world.item.crafting.RecipeHolder::value)
                *///?}
                ;
    }

    /** What the recipes read: the live grid on 1.20.1, a snapshot of it on 1.21. */
    //? if forge {
    private net.minecraft.world.inventory.CraftingContainer input() {
        return this.craft;
    }
    //?} else {
    /*private net.minecraft.world.item.crafting.CraftingInput input() {
        return this.craft.asCraftInput();
    }
    *///?}

    /** True if the network plus the crystals can cover the recipe's vis. */
    private boolean canPay(ArcaneCraftingRecipe recipe) {
        return payment(recipe, VisAction.SIMULATE) != null;
    }

    /**
     * The vis a recipe needs, split into what the network will give and what the crystals must cover. Null
     * when even together they fall short, or when there is no server level to ask.
     */
    @Nullable
    private Payment payment(ArcaneCraftingRecipe recipe, VisAction action) {
        Map<VisChannel, Integer> whole = recipe.vis(input());
        if (whole.isEmpty()) {
            return new Payment(Map.of(), Map.of());
        }
        if (!(this.player.level() instanceof ServerLevel level)) {
            return null;
        }
        Map<VisChannel, Integer> network = new java.util.EnumMap<>(VisChannel.class);
        Map<VisChannel, Integer> crystals = new java.util.EnumMap<>(VisChannel.class);
        for (Map.Entry<VisChannel, Integer> entry : whole.entrySet()) {
            int needed = entry.getValue() * 100;
            int drained = VisNetworkApi.drain(level, this.player.blockPosition(), entry.getKey(), needed, action);
            int shortfall = needed - drained;
            if (shortfall > 0) {
                if (crystalVis(entry.getKey()) < shortfall) {
                    return null;
                }
                crystals.put(entry.getKey(), shortfall);
            }
            if (drained > 0) {
                network.put(entry.getKey(), drained);
            }
        }
        return new Payment(network, crystals);
    }

    /** The whole vis the crystal slots can offer for one channel. */
    private int crystalVis(VisChannel channel) {
        int total = 0;
        for (int i = 0; i < this.crystals.getContainerSize(); i++) {
            if (crystalChannel(this.crystals.getItem(i)) == channel) {
                total += this.crystals.getItem(i).getCount() * CRYSTAL_VIS;
            }
        }
        return total;
    }

    /** Spends the ingredients and the vis once the player has committed to taking the result. */
    private void takeResult(Player player, ItemStack stack) {
        // Taking a crystal out re-runs the resolution, which clears these fields, so hold the recipe here.
        ArcaneCraftingRecipe arcane = this.arcane;
        CraftingRecipe vanilla = this.vanilla;
        if (arcane != null) {
            Payment payment = payment(arcane, VisAction.EXECUTE);
            if (payment == null) {
                updateResult();
                return;
            }
            for (Map.Entry<VisChannel, Integer> entry : payment.crystals().entrySet()) {
                spendCrystals(entry.getKey(), entry.getValue());
            }
            for (ItemStack remaining : arcane.getRemainingItems(input())) {
                if (!remaining.isEmpty() && !this.playerInventory.add(remaining)) {
                    player.drop(remaining, false);
                }
            }
        } else if (vanilla != null) {
            for (ItemStack remaining : vanilla.getRemainingItems(input())) {
                if (!remaining.isEmpty() && !this.playerInventory.add(remaining)) {
                    player.drop(remaining, false);
                }
            }
        } else {
            return;
        }
        for (int i = 0; i < this.craft.getContainerSize(); i++) {
            if (!this.craft.getItem(i).isEmpty()) {
                this.craft.removeItem(i, 1);
            }
        }
        updateResult();
    }

    private void spendCrystals(VisChannel channel, int centivis) {
        int left = centivis;
        for (int i = 0; i < this.crystals.getContainerSize() && left > 0; i++) {
            ItemStack crystal = this.crystals.getItem(i);
            if (crystalChannel(crystal) == channel) {
                int used = Math.min(crystal.getCount(), left / CRYSTAL_VIS);
                this.crystals.removeItem(i, used);
                left -= used * CRYSTAL_VIS;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_START, MENU_SIZE, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index >= PLAYER_START) {
            VisChannel channel = crystalChannel(stack);
            if (channel != null && this.moveItemStackTo(stack, CRYSTAL_START, PLAYER_START, false)) {
                // Taken by a crystal slot.
            } else if (!this.moveItemStackTo(stack, GRID_START, CRYSTAL_START, false)
                    && !this.moveItemStackTo(stack, index < HOTBAR_START ? HOTBAR_START : PLAYER_START,
                            index < HOTBAR_START ? MENU_SIZE : HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_START, MENU_SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Everything the fingers were holding is dropped, since there is no block to keep it in. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        for (int i = 0; i < this.craft.getContainerSize(); i++) {
            drop(this.craft.removeItemNoUpdate(i), player);
        }
        for (int i = 0; i < this.crystals.getContainerSize(); i++) {
            drop(this.crystals.removeItemNoUpdate(i), player);
        }
    }

    private void drop(ItemStack stack, Player player) {
        if (!stack.isEmpty() && !this.playerInventory.add(stack)) {
            player.drop(stack, false);
        }
    }

    /** What the network and the crystals will each pay, in centivis. */
    private record Payment(Map<VisChannel, Integer> network, Map<VisChannel, Integer> crystals) {
    }
}
