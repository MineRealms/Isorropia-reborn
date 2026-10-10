package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.client.fx.CreatureFx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A feline that sets every undead creature within five blocks alight. Upstream extended the 1.12 ocelot, which
 * was tameable; in 1.20.1 that role belongs to the cat, so the cat model, sitting and taming are reused.
 */
public class EntityGravekeeper extends Cat {
    private static final double AURA_RANGE = 5.0D;

    public EntityGravekeeper(EntityType<? extends Cat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 5.0F, false));
        this.goalSelector.addGoal(6, new CatSitOnBlockGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LeapAtTargetGoal(this, 0.3F));
        this.goalSelector.addGoal(8, new OcelotAttackGoal(this));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.8D, 1.0000001E-5F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Chicken.class, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 1, false, false,
                EntityGravekeeper::isUndead));
    }

    private static boolean isUndead(LivingEntity entity) {
        return entity.getMobType() == MobType.UNDEAD;
    }

    /** Undead are burned by the aura, not bitten. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living && isUndead(living)) {
            return true;
        }
        return target.hurt(this.damageSources().mobAttack(this), 2.0F);
    }

    @Override
    public void tick() {
        super.tick();
        boolean client = this.level().isClientSide;
        if (client && this.tickCount % 2 != 0) {
            return;
        }
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(AURA_RANGE),
                EntityGravekeeper::isUndead)) {
            if (client) {
                CreatureFx.gravekeeperBeam(this, entity);
            } else {
                entity.setSecondsOnFire(1);
            }
        }
    }
}
