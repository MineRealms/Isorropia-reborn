package fr.wind_blade.isorropia.common.compat.kubejs;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
//? if forge {
import dev.latvian.mods.kubejs.script.data.DataPackEventJS;
//?} else {
/*import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
*///?}

/** Writes ordinary data-pack recipes, so additions, overrides and removals participate in /reload. */
public final class HorizonsData {
    private HorizonsData() {
    }

    //? if forge {
    public static void creature(DataPackEventJS event, String id, JsonObject json) {
        event.addJson(path(id, "creature_infusions"), json.deepCopy());
    }

    public static void self(DataPackEventJS event, String id, JsonObject json) {
        event.addJson(path(id, "self_infusions"), json.deepCopy());
    }

    public static void removeCreature(DataPackEventJS event, String id) {
        creature(event, id, disabled());
    }

    public static void removeSelf(DataPackEventJS event, String id) {
        self(event, id, disabled());
    }
    //?} else {
    /*public static void creature(KubeDataGenerator event, String id, JsonObject json) {
        event.json(path(id, "creature_infusions"), json.deepCopy());
    }

    public static void self(KubeDataGenerator event, String id, JsonObject json) {
        event.json(path(id, "self_infusions"), json.deepCopy());
    }

    public static void removeCreature(KubeDataGenerator event, String id) {
        creature(event, id, disabled());
    }

    public static void removeSelf(KubeDataGenerator event, String id) {
        self(event, id, disabled());
    }
    *///?}

    private static ResourceLocation path(String id, String directory) {
        ResourceLocation recipe = ResourceLocation.parse(id);
        return ResourceLocation.fromNamespaceAndPath(recipe.getNamespace(), "isorropia/" + directory + "/" + recipe.getPath() + ".json");
    }

    private static JsonObject disabled() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", false);
        return json;
    }
}
