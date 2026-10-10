package fr.wind_blade.isorropia.common.sounds;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoundsIS {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Isorropia.MODID);

    public static final float CURATIVE_EXPUNGE_FLUX_START_LENGTH = 1.853F;
    public static final float CURATIVE_EXPUNGE_FLUX_LOOP_LENGTH = 5.821F;
    public static final float CURATIVE_EXPUNGE_FLUX_END_LENGTH = 1.747F;

    public static final RegistryObject<SoundEvent> CURATIVE_INFUSION_START = register("curative_infusion_start");
    public static final RegistryObject<SoundEvent> CURATIVE_INFUSION_LOOP = register("curative_infusion_loop");
    public static final RegistryObject<SoundEvent> CURATIVE_EXPUNGE_FLUX_START = register("curative_expunge_flux_start");
    public static final RegistryObject<SoundEvent> CURATIVE_EXPUNGE_FLUX_LOOP = register("curative_expunge_flux_loop");
    public static final RegistryObject<SoundEvent> CURATIVE_EXPUNGE_FLUX_END = register("curative_expunge_flux_end");

    private SoundsIS() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Isorropia.id(name)));
    }
}
