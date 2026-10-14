package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.focus.FocusId;
import dev.tc4port.thaumcraft.api.focus.FocusIdentityItem;
import dev.tc4port.thaumcraft.api.focus.FocusStateView;
import dev.tc4port.thaumcraft.api.focus.FocusUpgradeId;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.blocks.BlockLight;
import fr.wind_blade.isorropia.common.focus.IlluminationFocus;
import fr.wind_blade.isorropia.common.platform.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The illumination focus, mounted on a wand like any Thaumcraft focus. What it does lives in IlluminationFocus
 * and its costs and upgrade ranks in the focus catalog; the item only keeps the upgrades and the colour the
 * player chose for it.
 */
public class ItemFocusIllumination extends Item implements FocusIdentityItem {
    public static final FocusId ID = FocusId.of(Isorropia.id("focus_illumination"));
    private static final String UPGRADES = "FocusUpgrades";
    private static final String PROPERTIES = "FocusProperties";

    public ItemFocusIllumination(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public FocusId focusId(ItemStack stack) {
        return ID;
    }

    @Override
    public FocusStateView focusState(ItemStack stack) {
        CompoundTag tag = StackData.read(stack);
        if (tag.isEmpty()) {
            return new FocusStateView(ID, List.of());
        }
        List<FocusUpgradeId> upgrades = new ArrayList<>();
        for (Tag entry : tag.getList(UPGRADES, Tag.TAG_STRING)) {
            upgrades.add(FocusUpgradeId.parse(entry.getAsString()));
        }
        Map<ResourceLocation, String> properties = new LinkedHashMap<>();
        CompoundTag stored = tag.getCompound(PROPERTIES);
        for (String key : stored.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                properties.put(id, stored.getString(key));
            }
        }
        return new FocusStateView(ID, upgrades, properties);
    }

    /** The palette index the focus conjures in; TH kept it as the item's damage, the port as a focus property. */
    public static int colour(ItemStack stack) {
        CompoundTag tag = StackData.read(stack);
        String key = IlluminationFocus.COLOUR.toString();
        if (!tag.isEmpty() && tag.getCompound(PROPERTIES).contains(key)) {
            try {
                return Math.max(0, Math.min(BlockLight.COLOURS.length - 1,
                        Integer.parseInt(tag.getCompound(PROPERTIES).getString(key))));
            } catch (NumberFormatException ignored) {
            }
        }
        return IlluminationFocus.DEFAULT_COLOUR;
    }

    /** A copy of the focus set to another palette index, keeping its upgrades. */
    public static ItemStack withColour(ItemStack stack, int colour) {
        ItemStack copy = stack.copyWithCount(1);
        StackData.update(copy, tag -> {
            CompoundTag properties = tag.getCompound(PROPERTIES);
            properties.putString(IlluminationFocus.COLOUR.toString(), Integer.toString(colour));
            tag.put(PROPERTIES, properties);
        });
        return copy;
    }

    /** TH named each colour: "Wand Focus: Illumination (Red)". */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack) + "." + colour(stack));
    }

    @Override
    public ItemStack withFocusState(ItemStack stack, FocusStateView state) {
        ItemStack copy = stack.copyWithCount(1);
        StackData.update(copy, tag -> {
            ListTag upgrades = new ListTag();
            state.upgradeSlots().forEach(upgrade -> upgrades.add(StringTag.valueOf(upgrade.value().toString())));
            tag.put(UPGRADES, upgrades);
            CompoundTag properties = new CompoundTag();
            state.properties().forEach((key, value) -> properties.putString(key.toString(), value));
            tag.put(PROPERTIES, properties);
        });
        return copy;
    }
}
