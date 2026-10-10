package fr.wind_blade.isorropia.common.entities.ai;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Seeks dropped cobblestone within 16 blocks and swallows it one block at a time. */
public class EatStoneGoal extends Goal {
    private static final double RANGE = 16.0D;
    private final PathfinderMob mob;
    private final Runnable onEat;
    @Nullable
    private ItemEntity target;
    private int timeout;

    public EatStoneGoal(PathfinderMob mob, Runnable onEat) {
        this.mob = mob;
        this.onEat = onEat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = null;
        double best = RANGE * RANGE;
        for (ItemEntity item : this.mob.level().getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(RANGE),
                item -> item.isAlive() && item.getItem().is(Items.COBBLESTONE))) {
            double distance = item.distanceToSqr(this.mob);
            if (distance < best) {
                best = distance;
                this.target = item;
            }
        }
        return this.target != null;
    }

    @Override
    public void start() {
        this.timeout = 500;
        this.moveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return this.timeout-- > 0 && this.target != null && this.target.isAlive();
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
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (this.mob.distanceToSqr(this.target) <= 2.0D) {
            this.swallow(this.target);
        } else if (this.mob.getNavigation().isDone() || this.timeout % 10 == 0) {
            this.moveToTarget();
        }
    }

    private void moveToTarget() {
        if (this.target != null) {
            this.mob.getNavigation().moveTo(this.target, 1.0D + this.mob.getSpeed());
        }
    }

    private void swallow(ItemEntity item) {
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) {
            return;
        }
        this.onEat.run();
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        this.mob.playSound(SoundEvents.PLAYER_BURP, 0.2F, ((this.mob.getRandom().nextFloat() - this.mob.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
    }
}
