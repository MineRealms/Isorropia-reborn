package fr.wind_blade.isorropia.common.capabilities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.network.IsorropiaNetwork;
import fr.wind_blade.isorropia.common.network.SyncLivingDataPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Per-entity infusion state, replacing 1.12's LivingBaseCapability/LivingCapability pair: creature infusions
 * applied in the vat (with the organ each occupies), and for players the self infusions, the warp held by a
 * warped tumor and the two ability toggles.
 */
public class LivingData {
    public static final Capability<LivingData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation ID = Isorropia.id("living_data");

    private final LivingEntity entity;
    private final Map<ResourceLocation, CompoundTag> infusions = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> organs = new LinkedHashMap<>();
    private final Set<ResourceLocation> selfInfusions = new LinkedHashSet<>();
    private int tumorWarpPermanent;
    private int tumorWarpSticky;
    private int tumorWarpTemporary;
    private boolean climbDisabled;
    private boolean invisibilityDisabled;
    @Nullable
    private UUID owner;

    public LivingData(LivingEntity entity) {
        this.entity = entity;
    }

    public static Optional<LivingData> get(LivingEntity entity) {
        return entity.getCapability(CAPABILITY).resolve();
    }

    public boolean hasInfusion(ResourceLocation recipe) {
        return infusions.containsKey(recipe);
    }

    public Map<ResourceLocation, CompoundTag> getInfusions() {
        return Collections.unmodifiableMap(infusions);
    }

    public void addInfusion(ResourceLocation recipe, CompoundTag data) {
        infusions.put(recipe, data);
    }

    public void removeInfusion(ResourceLocation recipe) {
        infusions.remove(recipe);
        organs.values().removeIf(recipe::equals);
    }

    @Nullable
    public ResourceLocation getOrgan(String organ) {
        return organs.get(organ);
    }

    public boolean hasOrgan(String organ, ResourceLocation recipe) {
        return recipe.equals(organs.get(organ));
    }

    public void setOrgan(String organ, ResourceLocation recipe) {
        organs.put(organ, recipe);
    }

    public Set<ResourceLocation> getSelfInfusions() {
        return Collections.unmodifiableSet(selfInfusions);
    }

    public boolean hasSelfInfusion(ResourceLocation id) {
        return selfInfusions.contains(id);
    }

    public void addSelfInfusion(ResourceLocation id) {
        selfInfusions.add(id);
    }

    /** Self infusions die with the body that carried them. */
    public void resetSelfInfusions() {
        selfInfusions.clear();
        tumorWarpPermanent = 0;
        tumorWarpSticky = 0;
        tumorWarpTemporary = 0;
    }

    public int getTumorWarpPermanent() {
        return tumorWarpPermanent;
    }

    public int getTumorWarpSticky() {
        return tumorWarpSticky;
    }

    public int getTumorWarpTemporary() {
        return tumorWarpTemporary;
    }

    public int getTumorWarpTotal() {
        return tumorWarpPermanent + tumorWarpSticky + tumorWarpTemporary;
    }

    public void addTumorWarp(int permanent, int sticky, int temporary) {
        tumorWarpPermanent += permanent;
        tumorWarpSticky += sticky;
        tumorWarpTemporary += temporary;
    }

    public boolean isClimbDisabled() {
        return climbDisabled;
    }

    public void setClimbDisabled(boolean climbDisabled) {
        this.climbDisabled = climbDisabled;
    }

    public boolean isInvisibilityDisabled() {
        return invisibilityDisabled;
    }

    public void setInvisibilityDisabled(boolean invisibilityDisabled) {
        this.invisibilityDisabled = invisibilityDisabled;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /** Sends the whole state to the entity itself (if a player) and everyone tracking it. */
    public void sync() {
        if (entity.level().isClientSide) {
            return;
        }
        SyncLivingDataPacket packet = new SyncLivingDataPacket(entity.getId(), serialize());
        IsorropiaNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public void syncTo(ServerPlayer player) {
        IsorropiaNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncLivingDataPacket(entity.getId(), serialize()));
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        CompoundTag infusionTag = new CompoundTag();
        infusions.forEach((id, data) -> infusionTag.put(id.toString(), data.copy()));
        tag.put("Infusions", infusionTag);
        CompoundTag organTag = new CompoundTag();
        organs.forEach((organ, id) -> organTag.putString(organ, id.toString()));
        tag.put("Organs", organTag);
        ListTag selfTag = new ListTag();
        selfInfusions.forEach(id -> selfTag.add(StringTag.valueOf(id.toString())));
        tag.put("SelfInfusions", selfTag);
        tag.putInt("TumorWarpPermanent", tumorWarpPermanent);
        tag.putInt("TumorWarpSticky", tumorWarpSticky);
        tag.putInt("TumorWarpTemporary", tumorWarpTemporary);
        tag.putBoolean("ClimbDisabled", climbDisabled);
        tag.putBoolean("InvisibilityDisabled", invisibilityDisabled);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        infusions.clear();
        CompoundTag infusionTag = tag.getCompound("Infusions");
        for (String key : infusionTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                infusions.put(id, infusionTag.getCompound(key));
            }
        }
        organs.clear();
        CompoundTag organTag = tag.getCompound("Organs");
        for (String organ : organTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(organTag.getString(organ));
            if (id != null) {
                organs.put(organ, id);
            }
        }
        selfInfusions.clear();
        for (Tag element : tag.getList("SelfInfusions", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id != null) {
                selfInfusions.add(id);
            }
        }
        tumorWarpPermanent = tag.getInt("TumorWarpPermanent");
        tumorWarpSticky = tag.getInt("TumorWarpSticky");
        tumorWarpTemporary = tag.getInt("TumorWarpTemporary");
        climbDisabled = tag.getBoolean("ClimbDisabled");
        invisibilityDisabled = tag.getBoolean("InvisibilityDisabled");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final LivingData data;
        private final LazyOptional<LivingData> optional;

        public Provider(LivingEntity entity) {
            this.data = new LivingData(entity);
            this.optional = LazyOptional.of(() -> data);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return CAPABILITY.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serialize();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserialize(tag);
        }
    }
}
