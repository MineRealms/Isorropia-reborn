package fr.wind_blade.isorropia.common.blocks;

import dev.tc4port.thaumcraft.api.node.AuraNodeView;
import fr.wind_blade.isorropia.common.tiles.TileNodeMonitor;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Thaumic Horizons' node monitor, fixed to the face of an aura node. It can only be placed against a node, faces
 * away from it, and gives a strong redstone signal while the node is critical. The scanner itself is drawn by its
 * block entity renderer.
 */
public class BlockNodeMonitor extends Block implements EntityBlock {
    /** The way the monitor faces: away from the node behind it. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // The scanner sits against the node's face, a little under half a block deep.
        SHAPES.put(Direction.UP, Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0));
        SHAPES.put(Direction.DOWN, Block.box(3.0, 9.0, 3.0, 13.0, 16.0, 13.0));
        SHAPES.put(Direction.NORTH, Block.box(3.0, 3.0, 9.0, 13.0, 13.0, 16.0));
        SHAPES.put(Direction.SOUTH, Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 7.0));
        SHAPES.put(Direction.WEST, Block.box(9.0, 3.0, 3.0, 16.0, 13.0, 13.0));
        SHAPES.put(Direction.EAST, Block.box(0.0, 3.0, 3.0, 7.0, 13.0, 13.0));
    }

    public BlockNodeMonitor(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** Placed against a node: on the clicked face if a node is behind it, else on any face that has one. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        if (isNode(level, pos.relative(clicked.getOpposite()))) {
            return defaultBlockState().setValue(FACING, clicked);
        }
        for (Direction side : Direction.values()) {
            if (isNode(level, pos.relative(side))) {
                return defaultBlockState().setValue(FACING, side.getOpposite());
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isNode(level, pos.relative(state.getValue(FACING).getOpposite()));
    }

    private static boolean isNode(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AuraNodeView;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** A strong signal reaches through neighbouring blocks, so their neighbours are told as well, as TH did. */
    public static void updateAround(Level level, BlockPos pos, Block block) {
        level.updateNeighborsAt(pos, block);
        for (Direction side : Direction.values()) {
            level.updateNeighborsAt(pos.relative(side), block);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && state.getValue(POWERED)) {
            updateAround(level, pos, this);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileNodeMonitor(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TilesIS.NODE_MONITOR.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> TileNodeMonitor.clientTick(lvl, pos, st, (TileNodeMonitor) be)
                : (lvl, pos, st, be) -> TileNodeMonitor.serverTick(lvl, pos, st, (TileNodeMonitor) be);
    }
}
