package fr.wind_blade.isorropia.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigIS {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TAINT_EAT_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ORE_PIG_MAIN_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ORE_PIG_OTHER_WEIGHT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("The configuration for the creatures of the mod").push("creatures");
        TAINT_EAT_COOLDOWN = builder
                .comment("Ticks the taint pig waits before eating another taint block (20 ticks = 1 second)")
                .defineInRange("taint_eat_cooldown", 3, 0, 72000);
        ORE_PIG_MAIN_WEIGHT = builder
                .comment("Weight of the ore an ore pig was infused with in its nugget roll")
                .defineInRange("ore_pig_main_weight", 1.0D, 0.0D, 1000.0D);
        ORE_PIG_OTHER_WEIGHT = builder
                .comment("Weight of every other ore in an ore pig's nugget roll")
                .defineInRange("ore_pig_other_weight", 1.0D, 0.0D, 1000.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ConfigIS() {
    }
}
