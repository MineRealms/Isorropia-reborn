package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    public record Plane(int id, String name, Map<AspectId, Integer> aspects, List<ResourceLocation> effects,
            ResourceKey<Level> returnDimension, BlockPos returnVortex, int radius,
            @Nullable BlockPos portalAPos, @Nullable ResourceKey<Level> portalADim,
            @Nullable BlockPos portalBPos, @Nullable ResourceKey<Level> portalBDim,
            @Nullable BlockPos portalCPos, @Nullable ResourceKey<Level> portalCDim,
            @Nullable BlockPos portalDPos, @Nullable ResourceKey<Level> portalDDim) {

        /** TH's four gateway slots per plane: 1..4, or 0 when none is free. */
        public int firstFreePortal() {
            if (portalAPos == null) {
                return 1;
            }
            if (portalBPos == null) {
                return 2;
            }
            if (portalCPos == null) {
                return 3;
            }
            return portalDPos == null ? 4 : 0;
        }

        @Nullable
        public BlockPos portalPos(int which) {
            return switch (which) {
                case 1 -> portalAPos;
                case 2 -> portalBPos;
                case 3 -> portalCPos;
                case 4 -> portalDPos;
                default -> null;
            };
        }

        @Nullable
        public ResourceKey<Level> portalDim(int which) {
            return switch (which) {
                case 1 -> portalADim;
                case 2 -> portalBDim;
                case 3 -> portalCDim;
                case 4 -> portalDDim;
                default -> null;
            };
        }

        /** Fills one of the four slots, or clears it when the position is null. */
        public Plane withPortal(int which, @Nullable BlockPos pos, @Nullable ResourceKey<Level> dimension) {
            return switch (which) {
                case 1 -> new Plane(id, name, aspects, effects, returnDimension, returnVortex, radius, pos, dimension,
                        portalBPos, portalBDim, portalCPos, portalCDim, portalDPos, portalDDim);
                case 2 -> new Plane(id, name, aspects, effects, returnDimension, returnVortex, radius, portalAPos,
                        portalADim, pos, dimension, portalCPos, portalCDim, portalDPos, portalDDim);
                case 3 -> new Plane(id, name, aspects, effects, returnDimension, returnVortex, radius, portalAPos,
                        portalADim, portalBPos, portalBDim, pos, dimension, portalDPos, portalDDim);
                case 4 -> new Plane(id, name, aspects, effects, returnDimension, returnVortex, radius, portalAPos,
                        portalADim, portalBPos, portalBDim, portalCPos, portalCDim, pos, dimension);
                default -> this;
            };
        }
    }

    /** Reserves the next column, matching TH's {@code pocketPlaneMAXID} counter, and records the plane. */
    public int reserve(String name, Map<AspectId, Integer> aspects, List<ResourceLocation> effects,
            ResourceKey<Level> returnDimension, BlockPos returnVortex, int radius) {
        int id = nextId++;
        planes.put(id, new Plane(id, name, Map.copyOf(aspects), List.copyOf(effects), returnDimension, returnVortex,
                radius, null, null, null, null, null, null, null, null));
        setDirty();
        return id;
    }

    /** Records or clears one of a plane's four gateway slots. */
    public void setPortal(int planeId, int which, @Nullable BlockPos pos, @Nullable ResourceKey<Level> dimension) {
        Plane plane = planes.get(planeId);
        if (plane != null) {
            planes.put(planeId, plane.withPortal(which, pos, dimension));
            setDirty();
        }
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
            ListTag effectTags = new ListTag();
            for (ResourceLocation effect : plane.effects()) {
                effectTags.add(StringTag.valueOf(effect.toString()));
            }
            planeTag.put("effects", effectTags);
            ListTag portalsTag = new ListTag();
            for (int which = 1; which <= 4; which++) {
                BlockPos pos = plane.portalPos(which);
                ResourceKey<Level> dim = plane.portalDim(which);
                if (pos != null) {
                    CompoundTag portalTag = new CompoundTag();
                    portalTag.putInt("which", which);
                    portalTag.putLong("pos", pos.asLong());
                    portalTag.putString("dim", dim == null ? "" : dim.location().toString());
                    portalsTag.add(portalTag);
                }
            }
            planeTag.put("portals", portalsTag);
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
            List<ResourceLocation> effects = new ArrayList<>();
            ListTag effectTags = planeTag.getList("effects", Tag.TAG_STRING);
            for (int j = 0; j < effectTags.size(); j++) {
                ResourceLocation effect = ResourceLocation.tryParse(effectTags.getString(j));
                if (effect != null) {
                    effects.add(effect);
                }
            }
            ResourceLocation dimension = ResourceLocation.tryParse(planeTag.getString("returnDim"));
            int id = planeTag.getInt("id");
            BlockPos[] positions = new BlockPos[5];
            ResourceKey<?>[] dimensions = new ResourceKey[5];
            ListTag portalsTag = planeTag.getList("portals", Tag.TAG_COMPOUND);
            for (int j = 0; j < portalsTag.size(); j++) {
                CompoundTag portalTag = portalsTag.getCompound(j);
                int which = portalTag.getInt("which");
                if (which >= 1 && which <= 4) {
                    positions[which] = BlockPos.of(portalTag.getLong("pos"));
                    ResourceLocation portalDim = ResourceLocation.tryParse(portalTag.getString("dim"));
                    dimensions[which] = portalDim == null ? Level.OVERWORLD
                            : ResourceKey.create(Registries.DIMENSION, portalDim);
                }
            }
            store.planes.put(id, new Plane(id, planeTag.getString("name"), aspects, effects,
                    dimension == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(planeTag.getLong("returnVortex")), planeTag.getInt("radius"),
                    positions[1], dimensions[1], positions[2], dimensions[2], positions[3], dimensions[3],
                    positions[4], dimensions[4]));
        }
        return store;
    }
}
