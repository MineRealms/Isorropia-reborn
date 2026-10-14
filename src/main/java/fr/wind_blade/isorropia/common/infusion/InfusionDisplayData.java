package fr.wind_blade.isorropia.common.infusion;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server recipe snapshots for optional client recipe viewers, without a dependency on their APIs. */
public final class InfusionDisplayData {
    public record Entry(ResourceLocation id, boolean self, JsonObject json) {
    }

    private static volatile List<Entry> client = List.of();
    private static volatile Runnable listener;
    private static List<Entry> creature = List.of();
    private static List<Entry> self = List.of();

    private InfusionDisplayData() {
    }

    public static synchronized void serverRecipes(boolean selfRecipes, List<Entry> recipes) {
        if (selfRecipes) self = List.copyOf(recipes);
        else creature = List.copyOf(recipes);
    }

    public static synchronized List<Entry> serverEntries() {
        List<Entry> entries = new ArrayList<>(creature);
        entries.addAll(self);
        return List.copyOf(entries);
    }

    public static List<Entry> entries() {
        return client;
    }

    public static void accept(List<Entry> entries) {
        client = List.copyOf(entries);
        Runnable notify = listener;
        if (notify != null) notify.run();
    }

    public static void listen(Runnable callback) {
        listener = callback;
    }
}
