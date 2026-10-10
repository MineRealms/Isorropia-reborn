package fr.wind_blade.isorropia.common.entities.projectile;

import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

/** A short-lived, weightless spark that sets what it touches on fire. */
public class EntityEmber extends ThrowableProjectile {
    private int duration = 20;
    private int firey;
    private float damage = 1.0F;

    public EntityEmber(EntityType<? extends EntityEmber> type, Level level) {
        super(type, level);
    }

    public EntityEmber(Level level, LivingEntity shooter) {
        super(EntitiesIS.EMBER.get(), shooter, level);
        Vec3 look = shooter.getLookAngle();
        this.moveTo(shooter.getX() + look.x / 2.0D, shooter.getEyeY() + look.y / 2.0D, shooter.getZ() + look.z / 2.0D,
                shooter.getYRot(), shooter.getXRot());
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setFirey(int firey) {
        this.firey = firey;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (this.tickCount > this.duration) {
            this.discard();
            return;
        }
        double drag = this.duration <= 20 ? 0.95D : 0.975D;
        Vec3 motion = this.getDeltaMovement().scale(drag);
        if (this.onGround()) {
            motion = motion.scale(0.66D);
        }
        this.setDeltaMovement(motion);
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        DamageSource source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FIREBALL), this, this.getOwner());
        if (!target.fireImmune() && target.hurt(source, this.damage)) {
            target.setSecondsOnFire(3 + this.firey);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide || this.random.nextFloat() >= 0.025F * this.firey) {
            return;
        }
        boolean griefing = !(this.getOwner() instanceof Mob) || ForgeEventFactory.getMobGriefingEvent(this.level(), this.getOwner());
        BlockPos pos = result.getBlockPos().relative(result.getDirection());
        if (griefing && this.level().isEmptyBlock(pos)) {
            this.level().setBlockAndUpdate(pos, BaseFireBlock.getState(this.level(), pos));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", this.damage);
        tag.putInt("firey", this.firey);
        tag.putInt("duration", this.duration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("damage");
        this.firey = tag.getInt("firey");
        this.duration = tag.getInt("duration");
    }
}
