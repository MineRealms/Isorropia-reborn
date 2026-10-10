package fr.wind_blade.isorropia.common;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.items.misc.ItemCat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class IsorropiaCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Isorropia.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("isorropia", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.isorropia"))
            .icon(() -> ItemCat.create(ItemCat.EnumCat.CHICKEN))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> entry : ItemsIS.ITEMS.getEntries()) {
                    if (entry.getId().equals(ItemsIS.CAT.getId())) {
                        for (ItemCat.EnumCat cat : ItemCat.EnumCat.values()) {
                            if (cat.getRecipe() != null) {
                                output.accept(ItemCat.create(cat));
                            }
                        }
                    } else {
                        output.accept(entry.get());
                    }
                }
            })
            .build());

    private IsorropiaCreativeTabs() {
    }
}
