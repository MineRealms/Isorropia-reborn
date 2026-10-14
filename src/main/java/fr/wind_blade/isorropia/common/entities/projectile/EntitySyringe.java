package fr.wind_blade.isorropia.common.entities.projectile;

import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.items.misc.ItemSyringeInjection;
import fr.wind_blade.isorropia.common.platform.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thaumic Horizons' thrown syringe: an arrow in everything but name. It flies like one, strikes for its speed times
 * its base damage, sticks in the ground, can be picked up again as the injection it carried and, when it draws blood,
 * releases the injection's effects into whatever it hit. TH's injector and dispensers are what throw it.
 */
public class EntitySyringe extends AbstractArrow {
    /** The injection's colour, so the client can tint the syringe that is flying. TH sent this in its spawn packet. */
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(EntitySyringe.class,
            EntityDataSerializers.INT);
    /** TH's {@code damage} field: the multiplier on the syringe's speed. */
    private static final double BASE_DAMAGE = 1.5D;
    private static final String EFFECTS = "effects";
    private static final String COLOR = "color";

    /** The injection carried by this syringe, in TH's {@code effects} tag. Empty for a syringe that carries none. */
    private CompoundTag effects = new CompoundTag();

    public EntitySyringe(EntityType<? extends EntitySyringe> type, Level level) {
        super(type, level);
        this.setBaseDamage(BASE_DAMAGE);
    }

    /** TH's {@code EntitySyringe(world, thrower, speed, tag)}: the shoot, with the tag's effects along for the ride. */
    public EntitySyringe(Level level, LivingEntity shooter, float velocity, @Nullable CompoundTag effects) {
        this(EntitiesIS.SYRINGE.get(), level);
        this.carry(effects);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity * 1.5F, 1.0F);
        this.pickup = shooter instanceof Player ? Pickup.ALLOWED : Pickup.DISALLOWED;
    }

    /** TH's dispenser constructor: fired from a fixed point, and always retrievable. */
    public EntitySyringe(Level level, double x, double y, double z, @Nullable CompoundTag effects) {
        this(EntitiesIS.SYRINGE.get(), level);
        this.carry(effects);
        this.setPos(x, y, z);
        this.pickup = Pickup.ALLOWED;
    }

    private void carry(@Nullable CompoundTag tag) {
        this.effects = tag == null ? new CompoundTag() : tag.copy();
        this.setColor(ItemSyringeInjection.colorOf(this.effects));
    }

    //? if forge {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COLOR, ItemSyringeInjection.colorOf(null));
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, ItemSyringeInjection.colorOf(null));
    }
    *///?}

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    private void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    /** The injection this syringe carries, as an item stack, ready to be taken back out of the ground. */
    //? if forge {
    @Override
    protected ItemStack getPickupItem() {
        ItemStack injection = new ItemStack(ItemsIS.SYRINGE_INJECTION.get());
        injection.setTag(this.effects.copy());
        return injection;
    }
    //?} else {
    /*@Override
    protected ItemStack getDefaultPickupItem() {
        ItemStack injection = new ItemStack(ItemsIS.SYRINGE_INJECTION.get());
        StackData.write(injection, this.effects.copy());
        return injection;
    }
    *///?}

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float speed = (float) this.getDeltaMovement().length();
        int damage = net.minecraft.util.Mth.ceil(net.minecraft.util.Mth.clamp(speed * this.getBaseDamage(), 0.0D,
                Integer.MAX_VALUE));
        if (this.isCritArrow()) {
            damage += this.random.nextInt(damage / 2 + 2);
        }
        DamageSource source = this.damageSources().thrown(this, this.getOwner());
        if (this.isOnFire() && !(target instanceof EnderMan)) {
            target.setSecondsOnFire(5);
        }
        if (target.hurt(source, damage)) {
            if (target instanceof LivingEntity living) {
                if (!this.level().isClientSide) {
                    living.setArrowCount(living.getArrowCount() + 1);
                }
                if (this.getOwner() instanceof LivingEntity shooter) {
                    // 1.21 folded the knockback and post-hurt enchantment steps into the damage
                    // pipeline (AbstractArrow.doKnockback); the manual block below is forge-only.
                    //? if forge {
                    if (this.getKnockback() > 0) {
                        Vec3 motion = this.getDeltaMovement();
                        double flat = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                        if (flat > 0.0D) {
                            living.push(motion.x * this.getKnockback() * 0.6D / flat, 0.1D,
                                    motion.z * this.getKnockback() * 0.6D / flat);
                        }
                    }
                    EnchantmentHelper.doPostHurtEffects(living, shooter);
                    EnchantmentHelper.doPostDamageEffects(shooter, living);
                    //?} else {
                    /*this.doKnockback(living, source);
                    *///?}
                }
                if (this.getOwner() != null && target != this.getOwner() && target instanceof Player
                        && this.getOwner() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(
                            new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                }
            }
            this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (!(target instanceof EnderMan)) {
                this.discard();
                this.applyEffects(target);
            }
        } else {
            // TH's arrow bounced off a creature it failed to hurt.
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;
        }
    }

    /** TH's {@code applyEffects}: the injection's essences go into the creature the syringe struck. */
    private void applyEffects(Entity hit) {
        if (this.level().isClientSide || !(hit instanceof LivingEntity living)) {
            return;
        }
        List<MobEffectInstance> effects = ItemSyringeInjection.effects(this.getPickupItem());
        for (MobEffectInstance effect : effects) {
            living.addEffect(new MobEffectInstance(effect));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(EFFECTS, this.effects.copy());
        tag.putInt(COLOR, this.getColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.effects = tag.getCompound(EFFECTS);
        this.setColor(tag.contains(COLOR) ? tag.getInt(COLOR) : ItemSyringeInjection.colorOf(this.effects));
    }
}
