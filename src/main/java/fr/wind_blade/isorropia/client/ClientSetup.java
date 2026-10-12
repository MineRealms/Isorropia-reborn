package fr.wind_blade.isorropia.client;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.JellyColors;
import fr.wind_blade.isorropia.client.model.ModelJelly;
import fr.wind_blade.isorropia.client.model.ModelJellyRabbit;
import fr.wind_blade.isorropia.client.renderer.entities.RenderChickenVariant;
import fr.wind_blade.isorropia.client.renderer.entities.RenderChocolateCow;
import fr.wind_blade.isorropia.client.renderer.entities.RenderEmber;
import fr.wind_blade.isorropia.client.renderer.entities.RenderGravekeeper;
import fr.wind_blade.isorropia.client.renderer.entities.RenderGuardianPanther;
import fr.wind_blade.isorropia.client.renderer.entities.RenderHangingLabel;
import fr.wind_blade.isorropia.client.renderer.entities.RenderHellHound;
import fr.wind_blade.isorropia.client.renderer.entities.RenderJellyRabbit;
import fr.wind_blade.isorropia.client.renderer.entities.RenderOrePig;
import fr.wind_blade.isorropia.client.renderer.entities.RenderSheeder;
import fr.wind_blade.isorropia.client.renderer.entities.RenderTaintPig;
import fr.wind_blade.isorropia.client.model.MatrixCubeModel;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderDynamo;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderEtherealShard;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderJarSoul;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderNodeMonitor;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderModifiedMatrix;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderSoulBeacon;
import fr.wind_blade.isorropia.client.renderer.tiles.RenderVat;
import fr.wind_blade.isorropia.common.blocks.BlockLight;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.items.misc.ItemCat;
import fr.wind_blade.isorropia.common.items.misc.ItemJelly;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ItemsIS.CAT.get(), Isorropia.id("cat"),
                (stack, level, entity, seed) -> ItemCat.getType(stack).ordinal() / 100.0F));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
                fr.wind_blade.isorropia.common.menu.MenusIS.MORPHIC_FINGERS.get(),
                fr.wind_blade.isorropia.client.gui.MorphicFingersScreen::new));
        event.enqueueWork(fr.wind_blade.isorropia.client.fx.ContainmentFx::register);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModelJellyRabbit.LAYER, ModelJellyRabbit::createBodyLayer);
        event.registerLayerDefinition(ModelJelly.LAYER, ModelJelly::createBodyLayer);
        event.registerLayerDefinition(MatrixCubeModel.LAYER, MatrixCubeModel::createBodyLayer);
        event.registerLayerDefinition(fr.wind_blade.isorropia.client.model.EtherealShardModel.LAYER,
                fr.wind_blade.isorropia.client.model.EtherealShardModel::createBodyLayer);
        event.registerLayerDefinition(fr.wind_blade.isorropia.client.model.DynamoBaseModel.LAYER,
                fr.wind_blade.isorropia.client.model.DynamoBaseModel::createBodyLayer);
        event.registerLayerDefinition(fr.wind_blade.isorropia.client.model.ModelSomaticBrain.LAYER,
                fr.wind_blade.isorropia.client.model.ModelSomaticBrain::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntitiesIS.INCUBATED_EGG.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(EntitiesIS.TAINT_PIG.get(), RenderTaintPig::new);
        event.registerEntityRenderer(EntitiesIS.GRAVEKEEPER.get(), RenderGravekeeper::new);
        event.registerEntityRenderer(EntitiesIS.SHEEDER.get(), RenderSheeder::new);
        event.registerEntityRenderer(EntitiesIS.CHROMATIC_SHEEP.get(), SheepRenderer::new);
        event.registerEntityRenderer(EntitiesIS.SCHOLAR_CHICKEN.get(),
                context -> new RenderChickenVariant<>(context, Isorropia.id("textures/entity/scholar_chicken.png")));
        event.registerEntityRenderer(EntitiesIS.ORE_PIG.get(), RenderOrePig::new);
        event.registerEntityRenderer(EntitiesIS.JELLY_RABBIT.get(), RenderJellyRabbit::new);
        event.registerEntityRenderer(EntitiesIS.HANGING_LABEL.get(), RenderHangingLabel::new);
        event.registerEntityRenderer(EntitiesIS.CHOCOLATE_COW.get(), RenderChocolateCow::new);
        event.registerEntityRenderer(EntitiesIS.GOLDEN_CHICKEN.get(),
                context -> new RenderChickenVariant<>(context, Isorropia.id("textures/entity/golden_chicken.png")));
        event.registerEntityRenderer(EntitiesIS.GUARDIAN_PANTHER.get(), RenderGuardianPanther::new);
        event.registerEntityRenderer(EntitiesIS.NETHER_HOUND.get(), RenderHellHound::new);
        event.registerEntityRenderer(EntitiesIS.EMBER.get(), RenderEmber::new);
        event.registerEntityRenderer(EntitiesIS.ANIMATED_BLOCK.get(),
                fr.wind_blade.isorropia.client.renderer.entities.RenderAnimatedBlock::new);
        event.registerEntityRenderer(EntitiesIS.ALCHEMITE_PRIMED.get(),
                fr.wind_blade.isorropia.client.renderer.entities.RenderAlchemitePrimed::new);
    }

    @SubscribeEvent
    public static void onClientSetupCurios(FMLClientSetupEvent event) {
        event.enqueueWork(() -> top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                ItemsIS.SOMATIC_BRAIN.get(), fr.wind_blade.isorropia.client.curio.SomaticBrainCurioRenderer::new));
    }

    @SubscribeEvent
    public static void onRegisterOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(),
                "lens_wheel",
                (gui, graphics, partialTick, width, height) -> {
                    if (fr.wind_blade.isorropia.client.hud.LensWheel.isOpen()) {
                        fr.wind_blade.isorropia.client.hud.LensWheel.render(graphics, partialTick);
                    }
                });
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(),
                "lens_readouts",
                (gui, graphics, partialTick, width, height) ->
                        fr.wind_blade.isorropia.client.hud.LensHud.render(graphics, partialTick));
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(),
                "matrix_readout",
                (gui, graphics, partialTick, width, height) ->
                        fr.wind_blade.isorropia.client.hud.MatrixReadout.render(graphics));
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TilesIS.VAT.get(), RenderVat::new);
        event.registerBlockEntityRenderer(TilesIS.MODIFIED_MATRIX.get(), RenderModifiedMatrix::new);
        event.registerBlockEntityRenderer(TilesIS.JAR_SOUL.get(), RenderJarSoul::new);
        event.registerBlockEntityRenderer(TilesIS.SOUL_BEACON.get(), RenderSoulBeacon::new);
        // Thaumic Horizons' machines: both dynamos share one renderer, each with its own ring and base.
        event.registerBlockEntityRenderer(TilesIS.VIS_DYNAMO.get(), context -> new RenderDynamo<>(context,
                RenderDynamo.texture("goldring"), RenderDynamo.texture("dynamobase")));
        event.registerBlockEntityRenderer(TilesIS.ESSENTIA_DYNAMO.get(), context -> new RenderDynamo<>(context,
                RenderDynamo.texture("thaumiumring"), RenderDynamo.texture("dynamoessentiabase")));
        event.registerBlockEntityRenderer(TilesIS.NODE_MONITOR.get(), RenderNodeMonitor::new);
        event.registerBlockEntityRenderer(TilesIS.ETHEREAL_SHARD.get(), RenderEtherealShard::new);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF000000 | JellyColors.of(ItemJelly.getAspect(stack)) : -1,
                ItemsIS.JELLY.get());
    }

    /** A conjured mote wears the colour it was left in, so the block and the focus item never disagree. */
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? 0xFF000000 | BlockLight.COLOURS[state.getValue(BlockLight.COLOUR)]
                        : -1,
                BlocksIS.LIGHT.get());
    }
}
