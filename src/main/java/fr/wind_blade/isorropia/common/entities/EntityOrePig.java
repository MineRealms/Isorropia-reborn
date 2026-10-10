package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.entities.ai.EatStoneGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Eats cobblestone and, every hundred "percent" of digested stone, drops an ore nugget. */
public class EntityOrePig extends Pig {
    @Nullable
    private String ore;
    private int orePercent;

    public EntityOrePig(EntityType<? extends Pig> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(9, new EatStoneGoal(this, this::eatStone));
    }

    public void eatStone() {
        this.orePercent += this.random.nextInt(15) + 1;
        if (this.orePercent >= 100) {
            this.orePercent -= 100;
            ItemStack nugget = OrePigDrops.roll(this.ore, this.random);
            if (!nugget.isEmpty()) {
                this.spawnAtLocation(nugget);
            }
        }
    }

    /** The ore material (a {@code forge:ores/<material>} suffix) this pig favours; null for none. */
    @Nullable
    public String getOre() {
        return this.ore;
    }

    public void setOre(@Nullable String material) {
        this.ore = material == null || material.isEmpty() ? null : material;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OrePercent", this.orePercent);
        if (this.ore != null) {
            tag.putString("Ore", this.ore);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.orePercent = tag.getInt("OrePercent");
        this.setOre(tag.getString("Ore"));
    }
}
