package fr.wind_blade.isorropia.common.platform;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/** Mob calls whose signature differs between 1.20.1 and 1.21.1. */
public final class Mobs {
    private Mobs() {
    }

    /** Runs a freshly created mob's spawn setup; 1.21 dropped the spawn-NBT argument. */
    @Nullable
    public static SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason) {
        //? if forge {
        return mob.finalizeSpawn(level, difficulty, reason, null, null);
        //?} else {
        /*return mob.finalizeSpawn(level, difficulty, reason, null);
        *///?}
    }
}
