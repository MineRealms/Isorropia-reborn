package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlocksIS {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Isorropia.MODID);

    public static final RegistryObject<BlockCurativeVat> CURATIVE_VAT = BLOCKS.register("curative_vat",
            () -> new BlockCurativeVat(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                    .sound(SoundType.WOOD).lightLevel(state -> 8).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<BlockLiquidVat> VAT_INTERIOR = BLOCKS.register("vat_interior",
            () -> new BlockLiquidVat(BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(3.0F, 15.0F)
                    .lightLevel(state -> 8).noOcclusion().noCollission().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<BlockJarSoul> JAR_SOUL = BLOCKS.register("jar_soul",
            () -> new BlockJarSoul(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F)
                    .sound(SoundType.GLASS).lightLevel(state -> 10).noOcclusion().noLootTable()));
    public static final RegistryObject<Block> CHOCOLATE = BLOCKS.register("chocolate",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F, 10.0F)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockModifiedMatrix> MODIFIED_MATRIX = BLOCKS.register("modified_matrix",
            () -> new BlockModifiedMatrix(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F)
                    .sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<BlockSoulBeacon> SOUL_BEACON = BLOCKS.register("soul_beacon",
            () -> new BlockSoulBeacon(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(7.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 15).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<BlockLight> LIGHT = BLOCKS.register("light",
            () -> new BlockLight(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.0F)
                    .sound(SoundType.WOOL).noCollission().noOcclusion().noLootTable().instabreak()
                    .lightLevel(state -> state.getValue(BlockLight.SOLAR) ? 15 : 14)
                    .pushReaction(PushReaction.DESTROY)));

    private BlocksIS() {
    }
}
