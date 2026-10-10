package fr.wind_blade.isorropia.common.infusion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.capabilities.LivingData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Data-driven registry of the vat's creature and self infusions, plus the code that applies them. */
@Mod.EventBusSubscriber(modid = Isorropia.MODID)
public final class InfusionRecipes {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, CreatureInfusionRecipe> creature = Map.of();
    private static volatile Map<ResourceLocation, SelfInfusionRecipe> self = Map.of();

    private InfusionRecipes() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader<>("isorropia/creature_infusions", CreatureInfusionRecipe::fromJson, map -> creature = map));
        event.addListener(new Loader<>("isorropia/self_infusions", SelfInfusionRecipe::fromJson, map -> self = map));
    }

    public static Collection<CreatureInfusionRecipe> creatureRecipes() {
        return creature.values();
    }

    public static Optional<CreatureInfusionRecipe> creatureRecipe(ResourceLocation id) {
        return Optional.ofNullable(creature.get(id));
    }

    public static Optional<SelfInfusionRecipe> selfRecipe(ResourceLocation id) {
        return Optional.ofNullable(self.get(id));
    }

    public static Optional<CreatureInfusionRecipe> findCreature(@Nullable Player player, LivingEntity entity, List<ItemStack> components) {
        for (CreatureInfusionRecipe recipe : creature.values()) {
            if (knows(player, recipe.research()) && recipe.target().test(entity) && matches(components, recipe.components())) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static Optional<SelfInfusionRecipe> findSelf(@Nullable Player player, List<ItemStack> components) {
        for (SelfInfusionRecipe recipe : self.values()) {
            if (knows(player, recipe.research()) && matches(components, recipe.components())) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static boolean knows(@Nullable Player player, @Nullable ResearchKey research) {
        return research == null || (player != null && ResearchApi.isComplete(player, research));
    }

    /** Every pedestal item is used by exactly one component, as upstream's RecipeMatcher check. */
    private static boolean matches(List<ItemStack> inputs, List<net.minecraft.world.item.crafting.Ingredient> components) {
        return !components.isEmpty() && inputs.size() == components.size() && RecipeMatcher.findMatches(inputs, components) != null;
    }

    /**
     * Applies a finished creature infusion and returns the creature that now carries it (a new entity for SPECIE
     * recipes). {@code used} are the pedestal items consumed, for recipes whose result depends on them.
     */
    public static LivingEntity apply(CreatureInfusionRecipe recipe, LivingEntity entity, @Nullable Player player, List<ItemStack> used) {
        LivingEntity target = entity;
        if (recipe.kind() == CreatureInfusionRecipe.Kind.SPECIE && recipe.result() != null && entity.level() instanceof ServerLevel level) {
            Entity created = recipe.result().create(level);
            if (created instanceof LivingEntity living) {
                living.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                if (living instanceof Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.CONVERSION, null, null);
                    mob.setPersistenceRequired();
                }
                if (entity.hasCustomName()) {
                    living.setCustomName(entity.getCustomName());
                }
                LivingData.get(entity).ifPresent(old -> LivingData.get(living).ifPresent(data -> data.deserialize(old.serialize())));
                level.addFreshEntity(living);
                entity.discard();
                target = living;
            }
        }
        LivingData data = LivingData.get(target).orElse(null);
        if (data == null) {
            return target;
        }
        if (recipe.organ() != null) {
            ResourceLocation previous = data.getOrgan(recipe.organ().serializedName());
            if (previous != null) {
                remove(target, previous);
            }
            data.setOrgan(recipe.organ().serializedName(), recipe.id());
        }
        if (player != null) {
            data.setOwner(player.getUUID());
            if (target instanceof TamableAnimal tamable && !tamable.isTame()) {
                tamable.tame(player);
            }
        }
        for (CreatureInfusionRecipe.Modifier modifier : recipe.modifiers()) {
            AttributeInstance instance = target.getAttribute(modifier.attribute());
            if (instance != null && instance.getModifier(modifier.uuid()) == null) {
                instance.addPermanentModifier(modifier.create());
            }
        }
        data.addInfusion(recipe.id(), new CompoundTag());
        InfusionHooks.run(recipe.hook(), target, used);
        data.sync();
        return target;
    }

    /** Undoes an infusion's attribute modifiers and forgets it (used when an organ is replaced). */
    public static void remove(LivingEntity entity, ResourceLocation recipeId) {
        creatureRecipe(recipeId).ifPresent(recipe -> {
            for (CreatureInfusionRecipe.Modifier modifier : recipe.modifiers()) {
                AttributeInstance instance = entity.getAttribute(modifier.attribute());
                if (instance != null) {
                    instance.removeModifier(modifier.uuid());
                }
            }
        });
        LivingData.get(entity).ifPresent(data -> data.removeInfusion(recipeId));
    }

    private static final class Loader<T> extends SimpleJsonResourceReloadListener {
        private final String directory;
        private final Parser<T> parser;
        private final java.util.function.Consumer<Map<ResourceLocation, T>> sink;

        Loader(String directory, Parser<T> parser, java.util.function.Consumer<Map<ResourceLocation, T>> sink) {
            super(GSON, directory);
            this.directory = directory;
            this.parser = parser;
            this.sink = sink;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, T> loaded = new LinkedHashMap<>();
            prepared.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                try {
                    loaded.put(entry.getKey(), parser.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (RuntimeException e) {
                    Isorropia.LOGGER.error("Skipping {} {}: {}", directory, entry.getKey(), e.getMessage());
                }
            });
            sink.accept(java.util.Collections.unmodifiableMap(loaded));
            Isorropia.LOGGER.info("Loaded {} {}", loaded.size(), directory);
        }
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(ResourceLocation id, com.google.gson.JsonObject json);
    }
}
