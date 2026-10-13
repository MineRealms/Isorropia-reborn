package fr.wind_blade.isorropia.common.entities.projectile;

import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Thaumic Horizons' blast phial: a warded syringe's worth of treated blood mixed with gunpowder, thrown to burst over
 * everything nearby. In TH it was a copy of the vanilla splash potion with a fixed potion id; on 1.20.1 the potion
 * comes from the item the phial carries, so this class is only the phial's identity and its launch.
 */
public class EntityBlastPhial extends ThrownPotion {
    public EntityBlastPhial(EntityType<? extends EntityBlastPhial> type, Level level) {
        super(type, level);
    }

    /** TH's {@code EntityBlastPhial(world, thrower, power, stack)}: thrown from the thrower's eyes, carrying the injection. */
    public EntityBlastPhial(Level level, LivingEntity thrower, ItemStack stack) {
        super(EntitiesIS.BLAST_PHIAL.get(), level);
        this.setItem(stack);
        this.setOwner(thrower);
        this.setPos(thrower.getX(), thrower.getEyeY() - 0.1D, thrower.getZ());
    }

    /** TH's {@code EntityBlastPhial(world, x, y, z, stack)}: a dispenser's shot, from the dispenser's face. */
    public EntityBlastPhial(Level level, double x, double y, double z, ItemStack stack) {
        super(EntitiesIS.BLAST_PHIAL.get(), level);
        this.setItem(stack);
        this.setPos(x, y, z);
    }
}
