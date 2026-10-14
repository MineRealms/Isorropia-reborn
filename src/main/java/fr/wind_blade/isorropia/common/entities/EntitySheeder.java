package fr.wind_blade.isorropia.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A docile, grass-eating spider that grows a fleece of string. */
public class EntitySheeder extends Spider implements IForgeShearable {
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(EntitySheeder.class, EntityDataSerializers.BOOLEAN);

    public EntitySheeder(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    //? if forge {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHEARED, false);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEARED, false);
    }
    *///?}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.44D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(3, new EatBlockGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Skips the spider's jockey and potion-effect rolls. */
    //? if forge {
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data != null ? data : new SpawnGroupData() {
        }, tag);
        for (Entity passenger : List.copyOf(this.getPassengers())) {
            passenger.discard();
        }
        return result;
    }
    //?} else {
    /*@Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data != null ? data : new SpawnGroupData() { });
        for (Entity passenger : List.copyOf(this.getPassengers())) {
            passenger.discard();
        }
        return result;
    }
    *///?}

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return false;
    }

    public boolean isSheared() {
        return this.entityData.get(SHEARED);
    }

    public void setSheared(boolean sheared) {
        this.entityData.set(SHEARED, sheared);
    }

    @Override
    public void ate() {
        super.ate();
        this.setSheared(false);
    }

    //? if forge {
    @Override
    public boolean isShearable(@NotNull ItemStack item, Level level, BlockPos pos) {
        return this.isAlive() && !this.isSheared();
    }

    @Override
    public @NotNull List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos, int fortune) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        List<ItemStack> drops = new ArrayList<>();
        if (!level.isClientSide) {
            this.setSheared(true);
            int count = 2 + this.random.nextInt(4);
            for (int i = 0; i < count; i++) {
                drops.add(new ItemStack(Items.STRING));
            }
        }
        return drops;
    }
    //?} else {
    /*@Override
    public boolean isShearable(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos) {
        return this.isAlive() && !this.isSheared();
    }

    @Override
    public @NotNull List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        List<ItemStack> drops = new ArrayList<>();
        if (!level.isClientSide) {
            this.setSheared(true);
            int count = 2 + this.random.nextInt(4);
            for (int i = 0; i < count; i++) {
                drops.add(new ItemStack(Items.STRING));
            }
        }
        return drops;
    }
    *///?}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Sheared", this.isSheared());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSheared(tag.getBoolean("Sheared"));
    }
}
