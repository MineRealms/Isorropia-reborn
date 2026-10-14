package fr.wind_blade.isorropia.common.items.misc;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.platform.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Research icon for a creature infusion ("cat" = catalogue). The 1.12 metadata subtypes are an NBT
 * {@code Cat} string here; the client maps it to a model through the {@code isorropia:cat} item property.
 */
public class ItemCat extends Item {
    public static final String TAG = "Cat";

    public ItemCat() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack create(EnumCat type) {
        ItemStack stack = new ItemStack(ItemsIS.CAT.get());
        StackData.update(stack, tag -> tag.putString(TAG, type.getSerializedName()));
        return stack;
    }

    public static ItemStack create(EnumCat type, String name) {
        //? if forge {
        return create(type).setHoverName(Component.literal(name));
        //?} else {
        /*ItemStack stack = create(type);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
        *///?}
    }

    public static EnumCat getType(ItemStack stack) {
        CompoundTag tag = StackData.read(stack);
        return tag.isEmpty() ? EnumCat.PIG : EnumCat.byName(tag.getString(TAG));
    }

    /**
     * The catalogue is a research-page icon, so each subtype names itself after what it depicts: a
     * creature for the species entries, an infusion for the rest. Thaumic Horizons hid the item from
     * the creative menu entirely; the only place its name shows is the icon tooltip, and a single
     * shared "creature infusion" label there was just confusing.
     */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.isorropia.cat." + getType(stack).getSerializedName());
    }

    public enum EnumCat implements StringRepresentable {
        PIG("pig"),
        SHEEP("sheep"),
        COW("cow"),
        OCELOT("ocelot"),
        CHICKEN("chicken"),
        RABBIT("rabbit"),
        WOLF("wolf"),
        SPIDER("spider"),
        SQUID("squid"),
        LOVE("love", "isorropia:instilledfidelity"),
        SELFSHEARING("selfshearing", "isorropia:selfshearing"),
        ENDERHEART("enderheart", "isorropia:enderheart"),
        SHOCK("shockskin", "isorropia:shockskin"),
        AWAKENED_BLOOD("awakened_blood", "isorropia:awakened_blood"),
        DIAMOND_SKIN("diamond_skin", "isorropia:diamond_skin"),
        QUICKSILVER_LIMBS("quicksilver_limbs", "isorropia:quicksilver_limbs"),
        PORTABILITY("portability", "isorropia:portability"),
        STEVE("steve");

        private final String name;
        @Nullable
        private final String recipe;

        EnumCat(String name) {
            this(name, null);
        }

        EnumCat(String name, @Nullable String recipe) {
            this.name = name;
            this.recipe = recipe;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Nullable
        public String getRecipe() {
            return recipe;
        }

        public static EnumCat byName(String name) {
            for (EnumCat cat : values()) {
                if (cat.name.equals(name.toLowerCase(Locale.ROOT))) {
                    return cat;
                }
            }
            return PIG;
        }
    }
}
