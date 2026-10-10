package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** Lays gold eggs instead of eggs. */
public class EntityGoldenChicken extends Chicken {
    public EntityGoldenChicken(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        int eggTimer = this.eggTime;
        this.eggTime = Integer.MAX_VALUE;
        super.aiStep();
        this.eggTime = eggTimer;
        if (this.level().isClientSide || !this.isAlive() || this.isBaby() || this.isChickenJockey()) {
            return;
        }
        if (--this.eggTime <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(ItemsIS.GOLD_EGG.get());
            this.eggTime = this.random.nextInt(6000) + 6000;
        }
    }
}
