package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.focus.FocusId;
import dev.tc4port.thaumcraft.api.focus.FocusIdentityItem;
import dev.tc4port.thaumcraft.api.focus.FocusStateView;
import dev.tc4port.thaumcraft.api.focus.FocusUpgradeId;
import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.nbt.CompoundTag;
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
 * The animation focus, mounted on a wand like any Thaumcraft focus. What it does lives in AnimationFocus and its
 * costs and upgrade ranks in the focus catalog; the item only keeps the upgrades the player chose for it.
 */
public class ItemFocusAnimation extends Item implements FocusIdentityItem {
    public static final FocusId ID = FocusId.of(Isorropia.id("focus_animation"));
    private static final String UPGRADES = "FocusUpgrades";
    private static final String PROPERTIES = "FocusProperties";

    public ItemFocusAnimation(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public FocusId focusId(ItemStack stack) {
        return ID;
    }

    @Override
    public FocusStateView focusState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
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

    @Override
    public ItemStack withFocusState(ItemStack stack, FocusStateView state) {
        ItemStack copy = stack.copyWithCount(1);
        CompoundTag tag = copy.getOrCreateTag();
        ListTag upgrades = new ListTag();
        state.upgradeSlots().forEach(upgrade -> upgrades.add(StringTag.valueOf(upgrade.value().toString())));
        tag.put(UPGRADES, upgrades);
        CompoundTag properties = new CompoundTag();
        state.properties().forEach((key, value) -> properties.putString(key.toString(), value));
        tag.put(PROPERTIES, properties);
        return copy;
    }
}
