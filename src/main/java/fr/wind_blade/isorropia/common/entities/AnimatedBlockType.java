package fr.wind_blade.isorropia.common.entities;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The material a conjured golem is made of, as Thaumic Horizons classified the block it was called out of.
 * The numbers are upstream's own: health, armour points, walk speed, whether fire leaves it alone, how often it
 * knits a wound shut, how hard it hits, and the vis each primal costs to raise it.
 */
public enum AnimatedBlockType {
    GRASS(10, 0, 0.38F, false, 75, 0, 750),
    DIRT(10, 0, 0.38F, false, 75, 0, 500),
    WOOD(20, 6, 0.35F, false, 75, 1, 750),
    PLANT(10, 0, 0.4F, false, 75, 0, 750),
    ROCK(30, 12, 0.32F, true, 100, 3, 1000),
    METAL(35, 15, 0.31F, true, 125, 4, 1250),
    CLOTH(10, 0, 0.4F, false, 75, 0, 750),
    SAND(10, 0, 0.4F, false, 75, 0, 500),
    REDSTONE(20, 9, 0.35F, true, 45, 2, 1000),
    TNT(10, 0, 0.38F, false, 75, 0, 750),
    ICE(20, 6, 0.35F, false, 75, 1, 750),
    CACTUS(20, 6, 0.35F, false, 75, 1, 1000),
    CLAY(25, 9, 0.33F, true, 100, 2, 750),
    CAKE(20, 6, 0.35F, false, 25, 1, 750),
    WEB(15, 6, 0.35F, false, 40, 1, 1000),
    VOID(25, 15, 0.4F, true, 20, 3, 0);

    public final int health;
    public final int armor;
    public final float speed;
    public final boolean fireResist;
    public final int regenDelay;
    public final int strength;
    public final int visCost;

    AnimatedBlockType(int health, int armor, float speed, boolean fireResist, int regenDelay, int strength, int visCost) {
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.fireResist = fireResist;
        this.regenDelay = regenDelay;
        this.strength = strength;
        this.visCost = visCost;
    }

    /** The golem a block calls out of itself, mirroring upstream's material table. */
    public static AnimatedBlockType of(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
            return GRASS;
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MUD)) {
            return DIRT;
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES) || state.is(Blocks.CRAFTING_TABLE)) {
            return WOOD;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS) || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.VINE) || state.is(Blocks.LILY_PAD)) {
            return PLANT;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
            return CLOTH;
        }
        if (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SUSPICIOUS_SAND)) {
            return SAND;
        }
        if (state.is(Blocks.REDSTONE_BLOCK) || state.is(Blocks.REDSTONE_LAMP) || state.is(Blocks.REDSTONE_WIRE)
                || state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.OBSERVER) || state.is(Blocks.PISTON)
                || state.is(Blocks.STICKY_PISTON)) {
            return REDSTONE;
        }
        if (state.is(Blocks.TNT)) {
            return TNT;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) {
            return ICE;
        }
        if (state.is(Blocks.CACTUS)) {
            return CACTUS;
        }
        if (state.is(Blocks.CAKE)) {
            return CAKE;
        }
        if (state.is(Blocks.COBWEB)) {
            return WEB;
        }
        if (state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.COPPER_BLOCK)
                || state.is(Blocks.NETHERITE_BLOCK) || state.is(BlockTags.BEACON_BASE_BLOCKS)) {
            return METAL;
        }
        if (state.is(Blocks.CLAY) || state.is(Blocks.TERRACOTTA) || state.is(BlockTags.TERRACOTTA)) {
            return CLAY;
        }
        return ROCK;
    }
}
