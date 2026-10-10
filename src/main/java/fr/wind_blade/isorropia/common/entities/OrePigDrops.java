package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.config.ConfigIS;
import fr.wind_blade.isorropia.common.config.DuoWeightedRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The ore pig's droppings, derived from item tags the way 1.12 derived them from the ore dictionary: every
 * {@code forge:ores/<m>} that has a {@code forge:nuggets/<m>} (or, failing that, a {@code forge:dusts/<m>})
 * yields one entry. Rebuilt lazily after every tag reload.
 */
public final class OrePigDrops {
    public record Entry(String material, Item drop) {
    }

    @Nullable
    private static List<Entry> cache;

    private OrePigDrops() {
    }

    public static synchronized List<Entry> entries() {
        if (cache == null) {
            cache = build();
        }
        return cache;
    }

    public static synchronized void invalidate() {
        cache = null;
    }

    @Nullable
    public static Entry find(@Nullable String material) {
        if (material == null) {
            return null;
        }
        for (Entry entry : entries()) {
            if (entry.material().equals(material)) {
                return entry;
            }
        }
        return null;
    }

    public static ItemStack roll(@Nullable String mainMaterial, RandomSource random) {
        Entry entry = DuoWeightedRandom.pick(entries(), find(mainMaterial), ConfigIS.ORE_PIG_MAIN_WEIGHT.get(),
                ConfigIS.ORE_PIG_OTHER_WEIGHT.get(), random::nextDouble);
        return entry == null ? ItemStack.EMPTY : new ItemStack(entry.drop());
    }

    private static List<Entry> build() {
        Map<String, Item> nuggets = firstItems("nuggets/");
        Map<String, Item> dusts = firstItems("dusts/");
        Set<String> ores = new TreeSet<>(firstItems("ores/").keySet());
        List<Entry> entries = new ArrayList<>();
        for (String material : ores) {
            Item drop = nuggets.getOrDefault(material, dusts.get(material));
            if (drop != null) {
                entries.add(new Entry(material, drop));
            }
        }
        return List.copyOf(entries);
    }

    private static Map<String, Item> firstItems(String prefix) {
        Map<String, Item> result = new TreeMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            String namespace = pair.getFirst().location().getNamespace();
            String path = pair.getFirst().location().getPath();
            if (!"forge".equals(namespace) || !path.startsWith(prefix)) {
                return;
            }
            String material = path.substring(prefix.length());
            if (material.isEmpty() || material.contains("/")) {
                return;
            }
            pair.getSecond().stream().findFirst().map(Holder::value).ifPresent(item -> result.put(material, item));
        });
        return result;
    }
}
