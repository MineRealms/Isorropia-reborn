package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.tiles.TileRecombinator;
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

/**
 * Thaumic Horizons' teleological recombinator. It draws itself entirely with a block entity renderer (a clawed iron
 * pedestal holding a bobbing pearl), so the block shape is inert. A redstone signal turns it on; while on and after a
 * warm-up, the tile below it fuses primals drawn from nearby nodes into their compound aspects.
 */
public class BlockRecombinator extends Block implements EntityBlock {
    public BlockRecombinator(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TileRecombinator recombinator) {
                recombinator.setShouldActivate(level.hasNeighborSignal(pos));
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRecombinator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TilesIS.RECOMBINATOR.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, p, s, be) -> TileRecombinator.clientTick(lvl, p, s, (TileRecombinator) be)
                : (lvl, p, s, be) -> TileRecombinator.serverTick(lvl, p, s, (TileRecombinator) be);
    }
}
