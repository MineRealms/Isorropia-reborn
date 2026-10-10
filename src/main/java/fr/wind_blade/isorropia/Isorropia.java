package fr.wind_blade.isorropia;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Isorropia.MODID)
public class Isorropia {
    public static final String MODID = "isorropia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Isorropia() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
