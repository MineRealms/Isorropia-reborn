package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//? if neoforge {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?}
import java.util.function.Supplier;

public final class BlocksIS {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Isorropia.MODID);

    public static final Supplier<BlockCurativeVat> CURATIVE_VAT = BLOCKS.register("curative_vat",
            () -> new BlockCurativeVat(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                    .sound(SoundType.WOOD).lightLevel(state -> 8).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK)));
    public static final Supplier<BlockLiquidVat> VAT_INTERIOR = BLOCKS.register("vat_interior",
            () -> new BlockLiquidVat(BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(3.0F, 15.0F)
                    .lightLevel(state -> 8).noOcclusion().noCollission().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final Supplier<BlockJarSoul> JAR_SOUL = BLOCKS.register("jar_soul",
            () -> new BlockJarSoul(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F)
                    .sound(SoundType.GLASS).lightLevel(state -> 10).noOcclusion().noLootTable()));
    public static final Supplier<Block> CHOCOLATE = BLOCKS.register("chocolate",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F, 10.0F)
                    .requiresCorrectToolForDrops()));
    public static final Supplier<BlockModifiedMatrix> MODIFIED_MATRIX = BLOCKS.register("modified_matrix",
            () -> new BlockModifiedMatrix(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F)
                    .sound(SoundType.STONE).noOcclusion()));
    public static final Supplier<BlockSoulBeacon> SOUL_BEACON = BLOCKS.register("soul_beacon",
            () -> new BlockSoulBeacon(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(7.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 15).requiresCorrectToolForDrops().noOcclusion()));
    public static final Supplier<BlockLight> LIGHT = BLOCKS.register("light",
            () -> new BlockLight(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.0F)
                    .sound(SoundType.WOOL).noCollission().noOcclusion().noLootTable().instabreak()
                    .lightLevel(state -> state.getValue(BlockLight.SOLAR) ? 15 : 14)
                    .pushReaction(PushReaction.DESTROY)));
    public static final Supplier<BlockVisDynamo> VIS_DYNAMO = BLOCKS.register("vis_dynamo",
            () -> new BlockVisDynamo(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.7F, 1.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 8).requiresCorrectToolForDrops()));
    /** TH: iron, hardness 0.7, resistance 1.0, light 0.5 (level 7). */
    public static final Supplier<BlockEssentiaDynamo> ESSENTIA_DYNAMO = BLOCKS.register("essentia_dynamo",
            () -> new BlockEssentiaDynamo(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.7F, 1.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 7).noOcclusion().requiresCorrectToolForDrops()));
    /** TH: glass, hardness 0.7, resistance 1.0, light 0.5 (level 7); it needs no tool. */
    public static final Supplier<BlockNodeMonitor> NODE_MONITOR = BLOCKS.register("node_monitor",
            () -> new BlockNodeMonitor(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.7F, 1.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 7).noOcclusion()));
    /** TH: grass sound, hardness 0.7, resistance 1.0, light 0.5 (level 7), no tool needed. */
    public static final Supplier<BlockAlchemite> ALCHEMITE = BLOCKS.register("alchemite",
            () -> new BlockAlchemite(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.7F, 1.0F).sound(SoundType.GRASS).lightLevel(state -> 7)));
    /** TH: glass, hardness 0.7, resistance 1.0, light 0.5 (level 7); it needs no tool. */
    public static final Supplier<BlockEtherealShard> ETHEREAL_SHARD = BLOCKS.register("ethereal_shard",
            () -> new BlockEtherealShard(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                    .strength(0.7F, 1.0F).sound(SoundType.GLASS).lightLevel(state -> 7).noOcclusion()));
    /** TH: iron, hardness 0.7, resistance 1.0, light 0.5 (level 7). Points at the energized node it is placed on. */
    public static final Supplier<BlockTransductionAmplifier> TRANSDUCTION_AMPLIFIER = BLOCKS.register("transduction_amplifier",
            () -> new BlockTransductionAmplifier(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.7F, 1.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 7).noOcclusion().requiresCorrectToolForDrops()));
    /** TH: iron, hardness 10.0 (no explicit resistance). Fires a beam that quiets a hungry node. */
    public static final Supplier<BlockVortexStabilizer> VORTEX_STABILIZER = BLOCKS.register("vortex_stabilizer",
            () -> new BlockVortexStabilizer(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 7).noOcclusion().requiresCorrectToolForDrops()));
    /** TH: rock with no explicit hardness or resistance, so we give it the same as its sibling amplifier. */
    public static final Supplier<BlockRecombinator> RECOMBINATOR = BLOCKS.register("recombinator",
            () -> new BlockRecombinator(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.7F, 1.0F)
                    .sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops()));
    /** TH: cloth sound, hardness -1 (unbreakable), resistance 20000, light 15, no collision, drops nothing. */
    public static final Supplier<BlockVortex> VORTEX = BLOCKS.register("vortex",
            () -> new BlockVortex(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 20000.0F)
                    .sound(SoundType.WOOL).lightLevel(state -> 15).noCollission().noOcclusion().noLootTable()));
    /** TH: cloth sound, hardness -1 (unbreakable), resistance 60000, light 15. The pocket plane's shell. */
    public static final Supplier<BlockVoid> VOID = BLOCKS.register("void",
            () -> new BlockVoid(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 60000.0F)
                    .sound(SoundType.WOOL).lightLevel(state -> 15).noLootTable()));
    /** TH: sand material, hardness 0.5. The default ground of a pocket plane. */
    public static final Supplier<BlockDust> DUST = BLOCKS.register("dust",
            () -> new BlockDust(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F)
                    .sound(SoundType.SAND)));
    /** TH: rock, hardness 2.5, resistance 2.5, no drops. The ornate frame of a planar gateway. */
    public static final Supplier<BlockGatewayPortal> GATEWAY = BLOCKS.register("gateway",
            () -> new BlockGatewayPortal(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
                    .sound(SoundType.STONE).noLootTable()));
    /** TH: rock, hardness 2.5, resistance 2.5. The keystone receptacle at the top of a gateway. */
    public static final Supplier<BlockSlot> SLOT = BLOCKS.register("keystone_receptacle",
            () -> new BlockSlot(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
                    .sound(SoundType.STONE).noOcclusion()));
    /** TH: iron, hardness 1.0, resistance 1.0, not opaque. The blood syringe's dial and output rack. */
    public static final Supplier<BlockBloodInfuser> BLOOD_INFUSER = BLOCKS.register("blood_infuser",
            () -> new BlockBloodInfuser(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 1.0F)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    /** TH: unbreakable, no collision. The surface of a planar gateway, drawn by its block entity renderer. */
    public static final Supplier<BlockPortal> PORTAL = BLOCKS.register("portal",
            () -> new BlockPortal(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.WOOL).noCollission().noOcclusion().noLootTable()));

    private BlocksIS() {
    }
}
