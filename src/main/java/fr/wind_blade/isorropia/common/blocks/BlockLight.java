package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.tiles.TileLight;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Thaumic Horizons' conjured mote: the little light a wand leaves behind with the illumination focus. It gives
 * off light but has no body at all — it cannot be walked on, cannot be broken for anything, and any block
 * placed against it simply replaces it, which is how the original kept it from ever becoming litter.
 *
 * <p>A mote remembers which of the sixteen colours it was conjured in, and a solar mote burns the undead that
 * wander into it. TH never gave the block a model: what shows is the stream of wisps its tile throws off.
 */
public class BlockLight extends Block implements EntityBlock {
    /**
     * The conjured palette, in the order Thaumic Horizons kept it: one colour per dye. The focus' own colour is
     * an index into this, so the block and the item never disagree about what a colour looks like.
     */
    public static final int[] COLOURS = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320
    };

    /** Which of the palette's colours the mote was conjured in. */
    public static final IntegerProperty COLOUR = IntegerProperty.create("colour", 0, 15);
    /** A solar mote is a shade brighter and burns the undead. */
    public static final BooleanProperty SOLAR = BooleanProperty.create("solar");

    /** A mote is a small thing floating in the middle of the block, 0.3 to 0.7 as upstream shaped it. */
    private static final VoxelShape SHAPE = Block.box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    private static final int BURN_SECONDS = 5;

    public BlockLight(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOUR, 0).setValue(SOLAR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOUR, SOLAR);
    }

    /** TH's render type was never assigned, so the block itself drew nothing. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileLight(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && type == TilesIS.LIGHT.get()
                ? (lvl, pos, st, be) -> TileLight.clientTick(lvl, pos, st, (TileLight) be)
                : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Anything may be built straight through a mote, so it never has to be cleared away first. */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(SOLAR) && entity instanceof LivingEntity living && living.getMobType() == MobType.UNDEAD) {
            living.setSecondsOnFire(BURN_SECONDS);
        }
    }
}
