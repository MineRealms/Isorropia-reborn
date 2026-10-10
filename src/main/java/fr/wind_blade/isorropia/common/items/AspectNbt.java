package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/** Aspect contents stored on an item stack, in TC6's list layout: {@code Aspects: [{key, amount}, ...]}. */
public final class AspectNbt {
    public static final String ASPECTS_TAG = "Aspects";

    private AspectNbt() {
    }

    public static void write(CompoundTag tag, Map<AspectId, Integer> aspects) {
        ListTag list = new ListTag();
        aspects.forEach((id, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", id.serialized());
            entry.putInt("amount", amount);
            list.add(entry);
        });
        tag.put(ASPECTS_TAG, list);
    }

    /** Reads in stored order, merging duplicates and skipping malformed ids and non-positive amounts. */
    public static Map<AspectId, Integer> read(CompoundTag tag) {
        Map<AspectId, Integer> aspects = new LinkedHashMap<>();
        for (Tag element : tag.getList(ASPECTS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            int amount = entry.getInt("amount");
            if (amount <= 0) {
                continue;
            }
            try {
                aspects.merge(AspectId.parse(entry.getString("key")), amount, Integer::sum);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return aspects;
    }
}
