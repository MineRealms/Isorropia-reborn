package fr.wind_blade.isorropia.common.blocks;

import dev.tc4port.thaumcraft.api.wand.WandApi;
import fr.wind_blade.isorropia.common.tiles.TileModifiedMatrix;
import fr.wind_blade.isorropia.common.tiles.TileVat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** The vat's matrix: it only goes on top of a formed vat, and a wand is what starts the work. */
public class BlockModifiedMatrix extends Block implements EntityBlock {
    public BlockModifiedMatrix(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileModifiedMatrix(pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof TileVat vat && vat.isMaster();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos.below()) instanceof TileVat vat && vat.isActive()) {
            vat.setActive(false);
            if (vat.isInfusing()) {
                vat.destroyStructure();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !WandApi.view(player.getItemInHand(hand)).isPresent()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof TileModifiedMatrix matrix) {
            matrix.onWandUse(player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 7;
    }
}
