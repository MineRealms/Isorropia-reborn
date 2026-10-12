package fr.wind_blade.isorropia;

import com.mojang.logging.LogUtils;
import fr.wind_blade.isorropia.common.IsorropiaCreativeTabs;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.config.ConfigIS;
import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.focus.ContainmentFocus;
import fr.wind_blade.isorropia.common.focus.IlluminationFocus;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.menu.MenusIS;
import fr.wind_blade.isorropia.common.network.IsorropiaNetwork;
import fr.wind_blade.isorropia.common.sounds.SoundsIS;
import fr.wind_blade.isorropia.common.tiles.TilesIS;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Isorropia.MODID)
public class Isorropia {
    public static final String MODID = "isorropia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Isorropia(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        BlocksIS.BLOCKS.register(modBus);
        ItemsIS.ITEMS.register(modBus);
        SoundsIS.SOUNDS.register(modBus);
        TilesIS.TILES.register(modBus);
        MenusIS.MENUS.register(modBus);
        EntitiesIS.ENTITIES.register(modBus);
        modBus.addListener(EntitiesIS::onAttributes);
        IsorropiaCreativeTabs.TABS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, ConfigIS.SPEC);
        IsorropiaNetwork.init();
        // The wand predicts the beam on the client, so the action is registered on both sides.
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ContainmentFocus::register));
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(IlluminationFocus::register));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
