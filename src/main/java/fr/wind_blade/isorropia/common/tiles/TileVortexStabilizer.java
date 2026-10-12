package fr.wind_blade.isorropia.common.tiles;

import dev.tc4port.thaumcraft.api.node.NodeTypeId;
import dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Thaumic Horizons' vortex attenuator, ported to TC4R. Once every five ticks, while a redstone signal feeds it, it
 * casts a beam up to ten blocks along its facing. The first aura node the beam lands on is switched to a normal node,
 * so a hungry node stops pulling in and eating nearby matter; when the beam is broken or switched off (and no other
 * attenuator still holds that node) the node is returned to the type it had. TH also used this to hold open planar
 * vortexes; that half waits on the planar-vortex task, so only aura nodes are handled here.
 */
public class TileVortexStabilizer extends BlockEntity {
    /** How far the beam reaches, and how often the target is re-acquired. */
    private static final int RANGE = 10;
    private static final int SCAN_INTERVAL = 5;

    private boolean redstoned;
    private boolean hasTarget;
    private boolean fireOnce;
    /** The power state at the last scan, so a flip is acted on at once instead of waiting for the next scan. */
    private boolean lastScanRedstoned;
    private boolean scannedOnce;
    /** The type the targeted node had before we normalised it, to restore on release. */
    @Nullable
    private NodeTypeId previousType;
    /** The node the beam is holding, or null. */
    @Nullable
    private AuraNodeBlockEntity target;
    /** Where the beam currently lands, kept in sync for the renderer (client) and the other-stabilizer scan. */
    @Nullable
    private BlockPos targetPos;

    public TileVortexStabilizer(BlockPos pos, BlockState state) {
        super(TilesIS.VORTEX_STABILIZER.get(), pos, state);
    }

    public void setRedstoned(boolean value) {
        if (this.redstoned != value) {
            this.redstoned = value;
            setChanged();
        }
    }

    public boolean isRedstoned() {
        return redstoned;
    }

    public boolean hasBeam() {
        return redstoned && hasTarget;
    }

    @Nullable
    public BlockPos beamEnd() {
        return targetPos;
    }

    @Nullable
    private AuraNodeBlockEntity nodeAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node ? node : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileVortexStabilizer stabilizer) {
        if (!stabilizer.fireOnce) {
            stabilizer.redstoned = level.hasNeighborSignal(pos);
            stabilizer.fireOnce = true;
        }
        // Re-scan on the regular cadence, and at once whenever the power state has changed (so switching on or off
        // takes effect on the very next tick rather than waiting for the next scan).
        boolean powerChanged = !stabilizer.scannedOnce || stabilizer.redstoned != stabilizer.lastScanRedstoned;
        if (!powerChanged && level.getGameTime() % SCAN_INTERVAL != 0L) {
            return;
        }
        stabilizer.scannedOnce = true;
        stabilizer.lastScanRedstoned = stabilizer.redstoned;
        Direction facing = state.getValue(fr.wind_blade.isorropia.common.blocks.BlockVortexStabilizer.FACING);
        // Walk the beam outward one block at a time: a node is taken as the target even though it has no collision
        // shape, and any block that does have one stops the beam.
        AuraNodeBlockEntity found = null;
        BlockPos hitPos = null;
        if (stabilizer.redstoned) {
            for (int step = 1; step <= RANGE; step++) {
                BlockPos stepPos = pos.relative(facing, step);
                AuraNodeBlockEntity node = stabilizer.nodeAt(level, stepPos);
                if (node != null) {
                    found = node;
                    hitPos = stepPos;
                    break;
                }
                if (!level.getBlockState(stepPos).getCollisionShape(level, stepPos).isEmpty()) {
                    hitPos = stepPos;
                    break;
                }
            }
            if (hitPos == null) {
                hitPos = pos.relative(facing, RANGE);
            }
        }
        if (hitPos != null && !hitPos.equals(stabilizer.targetPos)) {
            // The beam moved: release the old node, then take the new one in the same tick.
            if (stabilizer.hasTarget) {
                stabilizer.reHungrifyTarget();
                stabilizer.hasTarget = false;
            }
            if (found != null) {
                stabilizer.hasTarget = true;
                stabilizer.target = found;
                stabilizer.previousType = found.nodeState().type();
                stabilizer.deHungrifyTarget();
            } else {
                stabilizer.target = null;
            }
            stabilizer.targetPos = hitPos;
            stabilizer.setChanged();
        } else if (hitPos == null && stabilizer.hasTarget) {
            stabilizer.reHungrifyTarget();
            stabilizer.hasTarget = false;
            stabilizer.setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileVortexStabilizer stabilizer) {
        // Nothing to advance; the renderer reads isRedstoned()/beamEnd() straight off the synced fields.
    }

    /** Suppress the targeted node's hungry behaviour by switching it to a normal node. */
    private void deHungrifyTarget() {
        if (target != null) {
            target.setNodeState(target.nodeState().withType(NodeTypeId.NORMAL));
            target.setChanged();
        }
    }

    /** Restore the node's original type, unless another attenuator is still holding it. */
    public void reHungrifyTarget() {
        if (target == null) {
            return;
        }
        if (previousType != null && !isNodeTargetedByOtherStabilizer()) {
            target.setNodeState(target.nodeState().withType(previousType));
            target.setChanged();
        }
    }

    /** True if another attenuator is aimed at the same node, so we must not put it back. */
    private boolean isNodeTargetedByOtherStabilizer() {
        if (targetPos == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= RANGE; distance++) {
                BlockEntity entity = level.getBlockEntity(targetPos.relative(direction.getOpposite(), distance));
                if (entity instanceof TileVortexStabilizer other && other != this && other.hasTarget
                        && targetPos.equals(other.targetPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("active", this.redstoned);
        tag.putBoolean("hasTarget", this.hasTarget);
        if (this.targetPos != null) {
            tag.putLong("targetPos", this.targetPos.asLong());
        }
        if (this.previousType != null) {
            tag.putString("prevType", this.previousType.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.redstoned = tag.getBoolean("active");
        this.hasTarget = tag.getBoolean("hasTarget");
        this.targetPos = tag.contains("targetPos") ? BlockPos.of(tag.getLong("targetPos")) : null;
        this.previousType = tag.contains("prevType") ? NodeTypeId.parse(tag.getString("prevType")) : null;
        this.fireOnce = true;
        this.scannedOnce = false;
    }
}
