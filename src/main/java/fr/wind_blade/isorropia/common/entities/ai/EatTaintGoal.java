package fr.wind_blade.isorropia.common.entities.ai;

import fr.wind_blade.isorropia.common.compat.TaintBridge;
import fr.wind_blade.isorropia.common.config.ConfigIS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Walks to nearby taint and eats it, turning it back into plain blocks and cleansing the biome column. */
public class EatTaintGoal extends Goal {
    private static final int GIVE_UP_TICKS = 200;
    private final PathfinderMob mob;
    @Nullable
    private BlockPos target;
    private int cooldown;
    private int ticks;

    public EatTaintGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        return this.findTaint();
    }

    private boolean findTaint() {
        this.target = null;
        BlockPos origin = this.mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -2, -5), origin.offset(5, 5, 5))) {
            if (this.isTaint(pos) && this.reachable(pos)) {
                this.target = pos.immutable();
                return true;
            }
        }
        for (int tries = 0; tries < 30; tries++) {
            BlockPos pos = origin.offset(this.mob.getRandom().nextInt(17) - 8, this.mob.getRandom().nextInt(5) - 2,
                    this.mob.getRandom().nextInt(17) - 8);
            if (this.mob.level().isEmptyBlock(pos.above()) && this.isTaint(pos) && this.reachable(pos)) {
                this.target = pos;
                return true;
            }
        }
        return false;
    }

    private boolean isTaint(BlockPos pos) {
        return TaintBridge.isTaint(this.mob.level().getBlockState(pos));
    }

    private boolean reachable(BlockPos pos) {
        return this.mob.getNavigation().createPath(pos, 0) != null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.moveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.ticks < GIVE_UP_TICKS && this.isTaint(this.target);
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks++;
        this.mob.getLookControl().setLookAt(this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D, 30.0F, 30.0F);
        if (this.mob.distanceToSqr(this.target.getCenter()) <= 4.0D) {
            this.eat(this.target);
        } else if (this.mob.getNavigation().isDone()) {
            this.moveToTarget();
        }
    }

    private void moveToTarget() {
        if (this.target != null) {
            this.mob.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D, 1.0D);
        }
    }

    private void eat(BlockPos pos) {
        BlockState state = this.mob.level().getBlockState(pos);
        this.target = null;
        if (!TaintBridge.isTaint(state) || !(this.mob.level() instanceof ServerLevel level)) {
            return;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, TaintBridge.cleansed(state), Block.UPDATE_ALL);
        TaintBridge.cleanseBiome(level, pos);
        this.mob.playSound(SoundEvents.PLAYER_BURP, 0.2F, ((this.mob.getRandom().nextFloat() - this.mob.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        this.mob.heal(1.0F);
        this.cooldown = ConfigIS.TAINT_EAT_COOLDOWN.get();
    }
}
