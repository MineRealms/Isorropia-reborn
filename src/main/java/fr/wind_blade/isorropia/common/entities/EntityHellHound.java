package fr.wind_blade.isorropia.common.entities;

import dev.tc4port.thaumcraft.api.ThaumcraftContent;
import fr.wind_blade.isorropia.common.entities.projectile.EntityEmber;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/** A fire-proof wolf that breathes a stream of embers at whatever it is attacking. */
public class EntityHellHound extends Wolf {
    private static final float SCATTER = 8.0F;
    private int soundDelay;

    public EntityHellHound(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = this.getTarget();
        if (this.level().isClientSide || target == null || !target.isAlive()) {
            return;
        }
        if (--this.soundDelay <= 0) {
            this.level().playSound(null, this.blockPosition(), ThaumcraftContent.sound("zap"), SoundSource.NEUTRAL, 0.33F, 2.0F);
            this.soundDelay = 10;
        }
        EntityEmber ember = new EntityEmber(this.level(), this);
        ember.setDamage(1.0F);
        ember.setFirey(1);
        ember.shootFromRotation(this, this.getXRot(), this.getYRot(), 0.0F, 1.0F, SCATTER);
        this.level().addFreshEntity(ember);
    }
}
