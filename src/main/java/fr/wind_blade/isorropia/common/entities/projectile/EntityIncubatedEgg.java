package fr.wind_blade.isorropia.common.entities.projectile;

import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A thrown incubated egg: always hatches one chick. */
public class EntityIncubatedEgg extends ThrowableItemProjectile {
    private static final byte HATCH_EVENT = 3;

    public EntityIncubatedEgg(EntityType<? extends EntityIncubatedEgg> type, Level level) {
        super(type, level);
    }

    public EntityIncubatedEgg(Level level, LivingEntity thrower) {
        super(EntitiesIS.INCUBATED_EGG.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemsIS.INCUBATED_EGG.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == HATCH_EVENT) {
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(),
                        (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        Chicken chick = EntityType.CHICKEN.create(this.level());
        if (chick != null) {
            chick.setAge(-24000);
            chick.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            this.level().addFreshEntity(chick);
        }
        this.level().broadcastEntityEvent(this, HATCH_EVENT);
        this.discard();
    }
}
