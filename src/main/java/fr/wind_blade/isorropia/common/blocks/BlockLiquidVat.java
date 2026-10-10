package fr.wind_blade.isorropia.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** The fluid column inside a formed vat; drawn by the vat renderer, never by the block model. */
public class BlockLiquidVat extends Block {
    public BlockLiquidVat(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
