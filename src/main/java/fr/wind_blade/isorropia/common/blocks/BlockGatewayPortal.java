package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.tiles.TileSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Thaumic Horizons' planar gateway, {@code BlockGatewayPortal}: the ornate frame the keystone receptacle turns a
 * five-by-five wall into. TH told the fifteen frame cells apart by block metadata and picked one of nine textures per
 * cell and per face; here that metadata is a {@link #VARIANT} property and the per-face choice lives in the fifteen
 * generated models, one per variant.
 *
 * <p>Breaking any frame cell closes the gateway it belongs to. TH found the receptacle from the broken cell's metadata;
 * the variant carries the same information, so the search is unchanged.</p>
 */
public class BlockGatewayPortal extends Block {
    /** TH's metadata 0..14, one per cell of the five-by-five ring that is not the receptacle. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 14);
    /**
     * True when the gateway's face runs along X, TH's {@code isXAligned}. TH read it back off the neighbouring blocks
     * at render time; a baked model cannot, so it is stored instead, and the blockstate turns the model a quarter
     * turn with it.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ALIGNED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("aligned");

    public BlockGatewayPortal(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0).setValue(ALIGNED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, ALIGNED);
    }

    /** TH's {@code breakBlock}: walk from the broken cell to the receptacle that owns this gateway, and close it. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        if (!(level instanceof ServerLevel)) {
            return;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int slotX;
        int slotY;
        int slotZ;
        int variant = state.getValue(VARIANT);
        if (variant < 5) {
            slotY = y + variant;
            if (isFrame(level, x + 1, y, z)) {
                slotX = x + 2;
                slotZ = z;
            } else {
                slotX = x;
                slotZ = z + 2;
            }
        } else if (variant == 5) {
            slotY = y;
            if (level.getBlockState(new BlockPos(x + 1, y, z)).is(BlocksIS.SLOT.get())) {
                slotX = x + 1;
                slotZ = z;
            } else {
                slotX = x;
                slotZ = z + 1;
            }
        } else if (variant == 8) {
            slotY = y;
            if (level.getBlockState(new BlockPos(x - 1, y, z)).is(BlocksIS.SLOT.get())) {
                slotX = x - 1;
                slotZ = z;
            } else {
                slotX = x;
                slotZ = z - 1;
            }
        } else if (variant == 6 || variant == 7 || variant == 9) {
            slotY = y + 4;
            if (level.getBlockState(new BlockPos(x + 1, y, z)).is(BlocksIS.GATEWAY.get())) {
                slotZ = z;
                slotX = switch (variant) {
                    case 6 -> x + 1;
                    case 9 -> x - 1;
                    default -> x;
                };
            } else {
                slotX = x;
                slotZ = switch (variant) {
                    case 6 -> z + 1;
                    case 9 -> z - 1;
                    default -> z;
                };
            }
        } else {
            slotY = y + variant - 10;
            if (isFrame(level, x - 1, y, z)) {
                slotX = x - 2;
                slotZ = z;
            } else {
                slotX = x;
                slotZ = z - 2;
            }
        }
        if (level.getBlockEntity(new BlockPos(slotX, slotY, slotZ)) instanceof TileSlot slot && slot.portalOpen()) {
            slot.destroyPortal();
        }
    }

    private static boolean isFrame(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.is(BlocksIS.GATEWAY.get()) || state.is(BlocksIS.PORTAL.get());
    }
}
