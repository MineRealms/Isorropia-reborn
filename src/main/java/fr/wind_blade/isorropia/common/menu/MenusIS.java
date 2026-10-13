package fr.wind_blade.isorropia.common.menu;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The menus the mod opens: the morphic fingers' workbench and the vis dynamo's primal switches. */
public final class MenusIS {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Isorropia.MODID);

    public static final RegistryObject<MenuType<MorphicFingersMenu>> MORPHIC_FINGERS = MENUS.register(
            "morphic_fingers", () -> new MenuType<>(MorphicFingersMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryObject<MenuType<VisDynamoMenu>> VIS_DYNAMO = MENUS.register(
            "vis_dynamo", () -> IForgeMenuType.create(VisDynamoMenu::new));
    public static final RegistryObject<MenuType<BloodInfuserMenu>> BLOOD_INFUSER = MENUS.register(
            "blood_infuser", () -> IForgeMenuType.create(BloodInfuserMenu::new));

    private MenusIS() {
    }
}
