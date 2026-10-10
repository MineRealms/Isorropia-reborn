package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.infusion.SoulJar;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.tiles.TileVat;
import fr.wind_blade.isorropia.common.vat.VatStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Every block of a formed vat. The 1.12 metadata variants are a blockstate property here: TOP carries the
 * master block entity, BOTTOM the slave, CONNECTOR an essentia port, GLASS and PLACEHOLDER the shell. Breaking
 * any of them takes the whole vat apart, as Thaumic Horizons' vat does.
 */
public class BlockCurativeVat extends Block implements EntityBlock {
    public static final EnumProperty<Type> VARIANT = EnumProperty.create("variant", Type.class);

    public BlockCurativeVat(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Type.PLACEHOLDER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVat(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof TileVat vat) {
                TileVat.serverTick(lvl, pos, st, vat);
            }
        };
    }

    /**
     * Right-clicking a vat block: an empty hand on an empty vat puts the player in, and a hand on the vat the
     * player is already in lets them out. Everything that arrives in a jar happens once the soul jar exists.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        TileVat master = masterAt(level, pos);
        if (master == null) {
            return InteractionResult.PASS;
        }
        if (master.getContained() == player) {
            master.releaseContained();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // A jar with a creature in it is how anything but the player gets into the vat.
        if (player.getItemInHand(hand).is(ItemsIS.JAR_SOUL.get()) && master.getContained() == null && !master.isInfusing()) {
            if (level.isClientSide) {
                return InteractionResult.sidedSuccess(true);
            }
            LivingEntity stored = SoulJar.take(player, player.getItemInHand(hand));
            if (stored != null) {
                stored.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stored.getYRot(), stored.getXRot());
                master.setContained(stored);
            }
            return InteractionResult.sidedSuccess(false);
        }
        if (master.getContained() != null || master.isInfusing()) {
            return InteractionResult.PASS;
        }
        if (VatStructure.isInterior(level, pos) || state.getValue(VARIANT) == Type.TOP) {
            master.setContained(player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    private static TileVat masterAt(Level level, BlockPos pos) {
        TileVat master = VatStructure.findMaster(level, pos);
        return master != null ? master : VatStructure.findMaster(level, pos.below(3));
    }

    /** The vat comes apart when any of its blocks goes, and the broken block itself still drops its material. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            TileVat master = masterAt(level, pos);
            if (master != null && !master.isBreaking()) {
                master.destroyStructure(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity be,
            ItemStack tool) {
        TileVat master = be instanceof TileVat vat ? vat.master() : null;
        if (master != null) {
            VatStructure.Part part = VatStructure.partFor(master.getBlockPos(), pos);
            if (part != VatStructure.Part.WATER) {
                Block.popResource(level, pos, new ItemStack(partItem(part)));
            }
        }
        super.playerDestroy(level, player, pos, state, be, tool);
    }

    private static Block partItem(VatStructure.Part part) {
        return switch (part) {
            case WOOD -> VatStructure.planks();
            case CONSTRUCT -> VatStructure.construct();
            case GLASS -> net.minecraft.world.level.block.Blocks.GLASS;
            case WATER -> net.minecraft.world.level.block.Blocks.AIR;
        };
    }

    public enum Type implements StringRepresentable {
        PLACEHOLDER("placeholder"),
        GLASS("glass"),
        TOP("top"),
        BOTTOM("bottom"),
        CONNECTOR("connector");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
