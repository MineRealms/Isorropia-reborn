package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Lays ink eggs and regularly sheds feathers. */
public class EntityScholarChicken extends Chicken {
    private int featherTime;

    public EntityScholarChicken(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
        this.featherTime = this.random.nextInt(4000) + 4000;
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
            this.playLaySound();
            this.spawnAtLocation(ItemsIS.INK_EGG.get());
            this.eggTime = this.random.nextInt(6000) + 6000;
        }
        if (--this.featherTime <= 0) {
            this.playLaySound();
            this.spawnAtLocation(Items.FEATHER);
            this.featherTime = this.random.nextInt(4000) + 4000;
        }
    }

    private void playLaySound() {
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FeatherLayTime", this.featherTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FeatherLayTime")) {
            this.featherTime = tag.getInt("FeatherLayTime");
        }
    }
}
