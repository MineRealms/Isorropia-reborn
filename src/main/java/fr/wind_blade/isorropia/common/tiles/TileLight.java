package fr.wind_blade.isorropia.common.tiles;

import fr.wind_blade.isorropia.client.fx.LightFx;
import fr.wind_blade.isorropia.common.blocks.BlockLight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * TH's {@code TileLight}: the conjured mote has no model of its own, and this tile exists only so the client can
 * keep the mote glowing with wisps of its colour. It holds no data.
 */
public class TileLight extends BlockEntity {
    public TileLight(BlockPos pos, BlockState state) {
        super(TilesIS.LIGHT.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileLight light) {
        int colour = BlockLight.COLOURS[state.getValue(BlockLight.COLOUR)];
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LightFx.tick(level, pos, colour));
    }
}
