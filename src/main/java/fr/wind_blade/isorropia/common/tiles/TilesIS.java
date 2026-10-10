package fr.wind_blade.isorropia.common.tiles;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * One type per block: every vat block (walls, connectors, bottom, and the master on top) is the same block,
 * so it is also the same block entity class, which decides from its own blockstate whether it is the master.
 */
public final class TilesIS {
    public static final DeferredRegister<BlockEntityType<?>> TILES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Isorropia.MODID);

    public static final RegistryObject<BlockEntityType<TileVat>> VAT = TILES.register("curative_vat",
            () -> BlockEntityType.Builder.of(TileVat::new, BlocksIS.CURATIVE_VAT.get()).build(null));
    public static final RegistryObject<BlockEntityType<TileModifiedMatrix>> MODIFIED_MATRIX = TILES.register("modified_matrix",
            () -> BlockEntityType.Builder.of(TileModifiedMatrix::new, BlocksIS.MODIFIED_MATRIX.get()).build(null));
    public static final RegistryObject<BlockEntityType<TileJarSoul>> JAR_SOUL = TILES.register("soul_jar",
            () -> BlockEntityType.Builder.of(TileJarSoul::new, BlocksIS.JAR_SOUL.get()).build(null));
    public static final RegistryObject<BlockEntityType<TileSoulBeacon>> SOUL_BEACON = TILES.register("soul_beacon",
            () -> BlockEntityType.Builder.of(TileSoulBeacon::new, BlocksIS.SOUL_BEACON.get()).build(null));

    private TilesIS() {
    }
}
