package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The registry of created pocket planes, TH's {@code PocketPlaneData.planes} plus its {@code pocketPlaneMAXID} counter.
 * TH kept both in a world-saved data file; here they live in the overworld's {@link SavedData}.
 *
 * <p>Only the bookkeeping is ported so far: the plane's identity, the aspects it was made from, the vortex it returns
 * to and its radius. TH's terrain stamping and portal pairing come with the terrain stage.</p>
 */
public class PocketPlaneStore extends SavedData {
    public static final String DATA_NAME = "isorropia_pocket_planes";

    private int nextId;
    private final Map<Integer, Plane> planes = new LinkedHashMap<>();

    /** One pocket plane. {@code returnVortex} is the rift in the world the visitor came from. */
    public record Plane(int id, String name, Map<AspectId, Integer> aspects,
            ResourceKey<Level> returnDimension, BlockPos returnVortex, int radius) {
    }

    /** Reserves the next column, matching TH's {@code pocketPlaneMAXID} counter, and records the plane. */
    public int reserve(String name, Map<AspectId, Integer> aspects, ResourceKey<Level> returnDimension,
            BlockPos returnVortex, int radius) {
        int id = nextId++;
        planes.put(id, new Plane(id, name, Map.copyOf(aspects), returnDimension, returnVortex, radius));
        setDirty();
        return id;
    }

    @Nullable
    public Plane plane(int id) {
        return planes.get(id);
    }

    public int planeCount() {
        return planes.size();
    }

    public int nextId() {
        return nextId;
    }

    public static PocketPlaneStore get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PocketPlaneStore::load, PocketPlaneStore::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", this.nextId);
        ListTag planesTag = new ListTag();
        for (Plane plane : this.planes.values()) {
            CompoundTag planeTag = new CompoundTag();
            planeTag.putInt("id", plane.id());
            planeTag.putString("name", plane.name());
            planeTag.putInt("radius", plane.radius());
            planeTag.putString("returnDim", plane.returnDimension().location().toString());
            planeTag.putLong("returnVortex", plane.returnVortex().asLong());
            ListTag aspectsTag = new ListTag();
            for (Map.Entry<AspectId, Integer> entry : plane.aspects().entrySet()) {
                CompoundTag aspectTag = new CompoundTag();
                aspectTag.putString("key", entry.getKey().serialized());
                aspectTag.putInt("amount", entry.getValue());
                aspectsTag.add(aspectTag);
            }
            planeTag.put("aspects", aspectsTag);
            planesTag.add(planeTag);
        }
        tag.put("planes", planesTag);
        return tag;
    }

    public static PocketPlaneStore load(CompoundTag tag) {
        PocketPlaneStore store = new PocketPlaneStore();
        store.nextId = tag.getInt("NextId");
        ListTag planesTag = tag.getList("planes", Tag.TAG_COMPOUND);
        for (int i = 0; i < planesTag.size(); i++) {
            CompoundTag planeTag = planesTag.getCompound(i);
            Map<AspectId, Integer> aspects = new LinkedHashMap<>();
            ListTag aspectsTag = planeTag.getList("aspects", Tag.TAG_COMPOUND);
            for (int j = 0; j < aspectsTag.size(); j++) {
                CompoundTag aspectTag = aspectsTag.getCompound(j);
                AspectId id = AspectId.parse(aspectTag.getString("key"));
                if (id != null) {
                    aspects.put(id, aspectTag.getInt("amount"));
                }
            }
            ResourceLocation dimension = ResourceLocation.tryParse(planeTag.getString("returnDim"));
            int id = planeTag.getInt("id");
            store.planes.put(id, new Plane(id, planeTag.getString("name"), aspects,
                    dimension == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(planeTag.getLong("returnVortex")), planeTag.getInt("radius")));
        }
        return store;
    }
}
