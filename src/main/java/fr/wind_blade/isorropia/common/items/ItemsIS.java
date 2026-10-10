package fr.wind_blade.isorropia.common.items;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.items.misc.ItemCat;
import fr.wind_blade.isorropia.common.items.misc.ItemChocolateBucket;
import fr.wind_blade.isorropia.common.items.misc.ItemLens;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ItemsIS {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Isorropia.MODID);

    public static final RegistryObject<Item> BASE_LENS = simple("lens");
    public static final RegistryObject<Item> CHOCOLATE_BAR = simple("chocolate_bar");
    public static final RegistryObject<Item> GOLD_EGG = simple("gold_egg");
    public static final RegistryObject<ItemLens> AIR_LENS = ITEMS.register("lens_air", ItemLens::new);
    public static final RegistryObject<ItemLens> ORDO_LENS = ITEMS.register("lens_ordo", ItemLens::new);
    public static final RegistryObject<ItemLens> FIRE_LENS = ITEMS.register("lens_fire", ItemLens::new);
    public static final RegistryObject<ItemLens> ENVY_LENS = ITEMS.register("lens_envy", ItemLens::new);
    public static final RegistryObject<Item> SOMATIC_BRAIN = ITEMS.register("somatic_brain",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INCUBATED_EGG = ITEMS.register("incubated_egg",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> INK_EGG = simple("ink_egg");
    public static final RegistryObject<Item> FLESH = simple("flesh");
    public static final RegistryObject<ItemCat> CAT = ITEMS.register("cat", ItemCat::new);
    public static final RegistryObject<Item> JELLY = simple("jelly");
    public static final RegistryObject<ItemChocolateBucket> CHOCOLATE_MILK_BUCKET = ITEMS.register("chocolate_milk_bucket",
            () -> new ItemChocolateBucket(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final RegistryObject<Item> CHOCOLATE_CREAM = ITEMS.register("chocolate_cream",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(3.0F).build())));
    public static final RegistryObject<Item> CORPSE_EFFIGY = ITEMS.register("corpse_effigy",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockItem> CHOCOLATE_BLOCK = blockItem("chocolate", BlocksIS.CHOCOLATE);
    public static final RegistryObject<BlockItem> MODIFIED_MATRIX = blockItem("modified_matrix", BlocksIS.MODIFIED_MATRIX);
    public static final RegistryObject<BlockItem> JAR_SOUL = blockItem("jar_soul", BlocksIS.JAR_SOUL);
    public static final RegistryObject<BlockItem> SOUL_BEACON = blockItem("soul_beacon", BlocksIS.SOUL_BEACON);

    private ItemsIS() {
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
