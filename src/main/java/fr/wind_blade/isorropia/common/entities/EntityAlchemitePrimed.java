package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.explosion.AlchemiteExplosion;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Thaumic Horizons' primed alchemite: TNT's throw, gravity and eighty-tick fuse, but the blast it leaves behind
 * is {@link AlchemiteExplosion} rather than a plain one.
 */
public class EntityAlchemitePrimed extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(EntityAlchemitePrimed.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE = 80;

    @Nullable
    private LivingEntity placer;

    public EntityAlchemitePrimed(EntityType<? extends EntityAlchemitePrimed> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public EntityAlchemitePrimed(Level level, double x, double y, double z, @Nullable LivingEntity placer) {
        this(EntitiesIS.ALCHEMITE_PRIMED.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * (Math.PI * 2.0D);
        this.setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
        this.setFuse(DEFAULT_FUSE);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.placer = placer;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (this.level() instanceof ServerLevel serverLevel) {
                AlchemiteExplosion.detonate(serverLevel, this.placer, this.getX(), this.getY(0.0625D), this.getZ());
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Fuse", (short) this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setFuse(tag.getShort("Fuse"));
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }
}
