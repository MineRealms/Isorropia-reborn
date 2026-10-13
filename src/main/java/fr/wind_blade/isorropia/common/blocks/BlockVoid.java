package fr.wind_blade.isorropia.common.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Thaumic Horizons' void block, the unbreakable, softly glowing bubble that forms the shell of a pocket plane. TH gave
 * it hardness -1, resistance 60000, a cloth step sound and light level 1.0 (15 here). Unlike TH's airy material it is a
 * normal solid block, so a visitor actually stands on the inside of the bubble.
 */
public class BlockVoid extends Block {
    public BlockVoid(Properties properties) {
        super(properties);
    }
}
