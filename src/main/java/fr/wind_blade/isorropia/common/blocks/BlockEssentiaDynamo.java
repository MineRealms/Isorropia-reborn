package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Thaumic Horizons' essentia dynamo. It has no screen: a tube below feeds it, a vis relay beside it takes the vis. */
public class BlockEssentiaDynamo extends Block implements EntityBlock {
    public BlockEssentiaDynamo(Properties properties) {
        super(properties);
    }

    /** The base and rings are drawn by the block entity renderer, as TH's dynamo was all TESR. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEssentiaDynamo(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TilesIS.ESSENTIA_DYNAMO.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> TileEssentiaDynamo.clientTick(lvl, pos, st, (TileEssentiaDynamo) be)
                : (lvl, pos, st, be) -> TileEssentiaDynamo.serverTick(lvl, pos, st, (TileEssentiaDynamo) be);
    }
}
