package fr.wind_blade.isorropia;

import com.mojang.logging.LogUtils;
import fr.wind_blade.isorropia.common.IsorropiaCreativeTabs;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.config.ConfigIS;
import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.network.IsorropiaNetwork;
import fr.wind_blade.isorropia.common.sounds.SoundsIS;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        EntitiesIS.ENTITIES.register(modBus);
        modBus.addListener(EntitiesIS::onAttributes);
        IsorropiaCreativeTabs.TABS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, ConfigIS.SPEC);
        IsorropiaNetwork.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
