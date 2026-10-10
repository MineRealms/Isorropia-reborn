package fr.wind_blade.isorropia.common.compat;

import dev.tc4port.thaumcraft.block.TaintSpreadLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Taint lookups that TC4R's public API does not cover. This is the only class that touches TC4R internals
 * ({@code dev.tc4port.thaumcraft.block.TaintSpreadLogic}, for biome purification).
 */
public final class TaintBridge {
    /** TC4R's own "is a taint block" tag (fibrous/crusted taint, tainted soil/grass/flesh/plant, spore stalks). */
    public static final TagKey<Block> TAINT = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft", "taint"));

    private TaintBridge() {
    }

    public static boolean isTaint(BlockState state) {
        return state.is(TAINT);
    }

    /** What a cleansed taint block turns back into: soil to dirt, crust to stone, growths to air. */
    public static BlockState cleansed(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath();
        return switch (path) {
            case "tainted_soil", "tainted_grass" -> Blocks.DIRT.defaultBlockState();
            case "crusted_taint" -> Blocks.STONE.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    /** Restores the original biome of the column at {@code pos} if TC4R tainted it. */
    public static void cleanseBiome(ServerLevel level, BlockPos pos) {
        if (TaintSpreadLogic.isTaintedAt(level, pos)) {
            TaintSpreadLogic.restoreOriginalBiomeColumn(level, pos);
        }
    }
}
