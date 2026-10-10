package fr.wind_blade.isorropia.common.entities;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import dev.tc4port.thaumcraft.api.flux.FluxConsumeContext;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.IsorropiaAspects;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.items.misc.ItemJelly;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A rabbit of living jelly that slowly grows layers of aspect-bearing jelly. While it has a layer, hits and
 * shears peel a layer off instead of hurting it. A vitium rabbit also feeds on nearby flux.
 */
public class EntityJellyRabbit extends Rabbit implements IForgeShearable {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(EntityJellyRabbit.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> JELLY = SynchedEntityData.defineId(EntityJellyRabbit.class, EntityDataSerializers.INT);
    private static final ResourceLocation FLUX_SOURCE = Isorropia.id("jelly_rabbit");
    public static final int GROW_TICKS = 8000;
    public static final int MAX_JELLY = 9;
    private static final int FEED_INTERVAL = 80;
    private static final Map<AspectId, String> NAMED = Map.of(
            IsorropiaAspects.POTENTIA, "potentia",
            IsorropiaAspects.VACUOS, "vacuos",
            IsorropiaAspects.TERRA, "terra",
            IsorropiaAspects.IGNIS, "bunnignis");
    private int growth;

    public EntityJellyRabbit(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ASPECT, IsorropiaAspects.ORDO.toString());
        this.entityData.define(JELLY, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        List<AspectId> aspects = new ArrayList<>(AspectApi.definitions().keySet());
        if (!aspects.isEmpty()) {
            aspects.sort((a, b) -> a.toString().compareTo(b.toString()));
            this.setAspect(aspects.get(this.random.nextInt(aspects.size())));
        }
        this.setJellySize(2);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.fallDistance = 0.0F;
        if (this.level().isClientSide || this.getJellySize() >= MAX_JELLY) {
            return;
        }
        if (this.growth % FEED_INTERVAL == 0 && IsorropiaAspects.VITIUM.equals(this.getAspect())
                && this.level() instanceof ServerLevel level) {
            BlockPos pos = this.blockPosition();
            FluxApi.consumeNearby(level, pos, 8, 10, FluxConsumeContext.machine(FLUX_SOURCE, pos), VisAction.EXECUTE);
        }
        if (++this.growth >= GROW_TICKS) {
            this.setJellySize(this.getJellySize() + 1);
            this.growth = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.getJellySize() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || this.isInvulnerableTo(source)) {
            return super.hurt(source, amount);
        }
        if (this.level().isClientSide || this.invulnerableTime > 10) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null) {
            double dx = attacker.getX() - this.getX();
            double dz = attacker.getZ() - this.getZ();
            while (dx * dx + dz * dz < 1.0E-4D) {
                dx = (Math.random() - Math.random()) * 0.01D;
                dz = (Math.random() - Math.random()) * 0.01D;
            }
            this.knockback(0.4D, dx, dz);
            if (attacker instanceof LivingEntity living && !(living instanceof Player player && player.isCreative())) {
                this.setLastHurtByMob(living);
            }
        }
        this.playSound(SoundEvents.SLIME_HURT, 1.0F, this.getVoicePitch());
        this.spawnAtLocation(this.peelJelly());
        this.invulnerableTime = 20;
        return false;
    }

    /** Removes one jelly layer and returns it as an item. */
    public ItemStack peelJelly() {
        ItemStack stack = ItemJelly.create(ItemsIS.JELLY.get(), this.getAspect());
        this.setJellySize(this.getJellySize() - 1);
        return stack;
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, Level level, BlockPos pos) {
        return this.getJellySize() > 0;
    }

    @Override
    public @NotNull List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos, int fortune) {
        if (level.isClientSide) {
            return List.of();
        }
        return List.of(this.peelJelly());
    }

    @Nullable
    public AspectId getAspect() {
        String value = this.entityData.get(ASPECT);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return AspectId.parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setAspect(@Nullable AspectId aspect) {
        this.entityData.set(ASPECT, aspect == null ? "" : aspect.toString());
    }

    /** Aspect colour for rendering, or -1 if the aspect is unknown on this side. */
    public int getAspectColor() {
        AspectId aspect = this.getAspect();
        AspectDefinition definition = aspect == null ? null : AspectApi.get(aspect);
        return definition == null ? -1 : definition.color();
    }

    public int getJellySize() {
        return this.entityData.get(JELLY);
    }

    public void setJellySize(int size) {
        this.entityData.set(JELLY, Math.max(0, size));
    }

    @Override
    protected Component getTypeName() {
        AspectId aspect = this.getAspect();
        if (aspect == null) {
            return super.getTypeName();
        }
        String named = NAMED.get(aspect);
        if (named != null) {
            return Component.translatable("entity.isorropia.jelly_rabbit." + named);
        }
        return Component.translatable("entity.isorropia.jelly_rabbit.aspect", AspectApi.tooltipName(aspect));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Aspect", this.entityData.get(ASPECT));
        tag.putInt("Jelly", this.getJellySize());
        tag.putInt("Growth", this.growth);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Aspect")) {
            this.entityData.set(ASPECT, tag.getString("Aspect"));
        }
        this.setJellySize(tag.getInt("Jelly"));
        this.growth = tag.getInt("Growth");
    }
}
