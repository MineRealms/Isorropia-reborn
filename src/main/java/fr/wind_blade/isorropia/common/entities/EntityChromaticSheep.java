package fr.wind_blade.isorropia.common.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/** A sheep whose fleece steps through the sixteen dye colours every 30 ticks. */
public class EntityChromaticSheep extends Sheep {
    public EntityChromaticSheep(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.tickCount % 30 == 0) {
            this.setColor(DyeColor.byId((this.getColor().getId() + 1) % 16));
        }
        super.aiStep();
    }
}
