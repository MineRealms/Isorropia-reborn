package fr.wind_blade.isorropia.common.tiles;

import dev.tc4port.thaumcraft.api.node.AuraNodeState;
import dev.tc4port.thaumcraft.api.node.AuraNodeView;
import fr.wind_blade.isorropia.common.blocks.BlockNodeMonitor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Thaumic Horizons' node monitor: it watches the aura node it is fixed to and gives a full redstone signal as soon as
 * any of the node's aspects is down to one point of vis or less, so a node being drained can switch its drain off.
 * Taken off its node, it drops.
 */
public class TileNodeMonitor extends BlockEntity {
    /** An aspect at or below this is critical, as TH judged it. */
    public static final int CRITICAL = 1;
    /** The screen blinks this often, in the viewer's ticks, while the node is critical. */
    private static final int BLINK_TICKS = 15;

    // Client only: the scanner head turns while all is well, and the screen blinks when it is not.
    private int rotation;
    private int lastRotation;
    private boolean blink;

    public TileNodeMonitor(BlockPos pos, BlockState state) {
        super(TilesIS.NODE_MONITOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileNodeMonitor monitor) {
        AuraNodeView node = nodeFor(level, pos, state);
        if (node == null) {
            // TH's killMe: the monitor falls off a node that is no longer there.
            level.destroyBlock(pos, true);
            return;
        }
        boolean critical = isCritical(node.nodeState());
        if (critical != state.getValue(BlockNodeMonitor.POWERED)) {
            level.setBlock(pos, state.setValue(BlockNodeMonitor.POWERED, critical), 3);
            BlockNodeMonitor.updateAround(level, pos, state.getBlock());
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNodeMonitor monitor) {
        monitor.lastRotation = monitor.rotation;
        if (!state.getValue(BlockNodeMonitor.POWERED)) {
            monitor.blink = false;
            monitor.rotation++;
            if (monitor.rotation > 360) {
                monitor.rotation -= 360;
                monitor.lastRotation -= 360;
            }
        } else if (level.getGameTime() % BLINK_TICKS == 0L) {
            monitor.blink = !monitor.blink;
        }
    }

    /** The node the monitor is fixed to: the block behind its face. */
    @Nullable
    public static AuraNodeView nodeFor(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockNodeMonitor.FACING);
        return level.getBlockEntity(pos.relative(facing.getOpposite())) instanceof AuraNodeView node ? node : null;
    }

    /** True when any aspect the node carries, even one drained to nothing, is at one point or less. */
    public static boolean isCritical(AuraNodeState state) {
        return state.currentVis().amounts().values().stream().anyMatch(amount -> amount <= CRITICAL);
    }

    public float rotation(float partialTick) {
        return lastRotation + (rotation - lastRotation) * partialTick;
    }

    public boolean blink() {
        return blink;
    }
}
