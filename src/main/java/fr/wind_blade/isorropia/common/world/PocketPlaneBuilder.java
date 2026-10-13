package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.tiles.TileVortex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * Stamps a pocket plane into the void, the way TH's {@code PocketPlaneData.generatePocketPlane} did. TH did not use a
 * chunk generator for this: it wrote the island straight into the chunks, and this port keeps that approach, writing
 * through {@link LevelChunk#setBlockState} so a 127-radius shell does not cost a block update per block.
 *
 * <p>Stage 2a covers the frame of a plane: the aspect rings, the void bubble around them, the landing pad and the exit
 * rift. TH's layered terrain (layers, caves, pockets, ravines, clouds, surface and underground features, bones) still
 * has to land on top of it.</p>
 */
public final class PocketPlaneBuilder {
    /** TH's radius floor and ceiling. */
    private static final int MIN_RADIUS = 32;
    private static final int MAX_RADIUS = 127;

    private PocketPlaneBuilder() {
    }

    /**
     * Creates a plane for the given aspects and records it. Returns its id, which is also its column along +Z.
     *
     * <p>The order is TH's {@code generatePocketPlane}: pick the biome (the terrain dressing reads it), lay the
     * layers, carve the caves and pockets, fill the plane's effect list, cut the aspect rings into the terrain, wrap
     * the whole thing in the void bubble, write the biome over the chunks that were stamped, then carve the landing
     * pad and plant the exit rift.</p>
     */
    public static int create(ServerLevel pocket, PocketPlaneStore store, String name, Map<AspectId, Integer> aspects,
            ResourceKey<Level> returnDimension, BlockPos returnVortex) {
        int id = store.nextId();
        BlockPos centre = PocketPlane.anchor(id);
        int radius = radiusFor(aspects);

        // TH chose the biome before anything else, because the terrain dressing reads it.
        ResourceKey<Biome> biome = PocketPlaneBiomes.choose(aspects);
        boolean coldBiome = PocketPlaneBiomes.isCold(aspects);

        PocketPlaneTerrain.drawLayers(pocket, radius, centre, aspects, coldBiome);
        PocketPlaneTerrain.drawCavesAndPockets(pocket, radius, centre, aspects);

        // TH filled the plane's effect list here and never read it back anywhere; the list is kept on the plane.
        List<ResourceLocation> effects = PocketPlaneEffects.forAspects(aspects).stream()
                .map(BuiltInRegistries.MOB_EFFECT::getKey).filter(java.util.Objects::nonNull).toList();

        // TH laid one shell per "resonant" aspect inside the bubble, deepest first.
        int rings = 0;
        if (has(aspects, "alienis")) {
            shell(pocket, centre, radius - ++rings, BlocksIS.VOID.get().defaultBlockState());
        }
        if (has(aspects, "tenebrae")) {
            shell(pocket, centre, radius - ++rings, Blocks.DIRT.defaultBlockState());
        }
        if (has(aspects, "alienis")) {
            shell(pocket, centre, radius - ++rings, Blocks.STONE.defaultBlockState());
        }
        if (has(aspects, "vitreus")) {
            shell(pocket, centre, radius - ++rings, Blocks.GLASS.defaultBlockState());
        }
        if (has(aspects, "gelum")) {
            shell(pocket, centre, radius - ++rings, Blocks.PACKED_ICE.defaultBlockState());
        }
        shell(pocket, centre, radius, BlocksIS.VOID.get().defaultBlockState());

        // The biome goes over the chunks the terrain actually stamped, so a plane reads as its own climate.
        PocketPlaneBiomes.apply(pocket, centre, radius, biome);

        // A five-by-five arcane-stone landing pad with the doorway above it, exactly as TH carved out.
        BlockState pad = tc("arcane_stone");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                setFast(pocket, centre.getX() + dx, centre.getY(), centre.getZ() + dz, pad);
                setFast(pocket, centre.getX() + dx, centre.getY() + 1, centre.getZ() + dz, Blocks.AIR.defaultBlockState());
                setFast(pocket, centre.getX() + dx, centre.getY() + 2, centre.getZ() + dz, Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos doorway = PocketPlane.exit(id);
        setFast(pocket, doorway.getX(), doorway.getY(), doorway.getZ(), BlocksIS.VORTEX.get().defaultBlockState());
        if (pocket.getBlockEntity(doorway) instanceof TileVortex vortex) {
            // TH's exit rift is a cheat rift: it shows the way home instead of eating the island.
            vortex.setCheat(true);
            vortex.setAspects(aspects);
            vortex.setPlaneId(id);
        }

        store.reserve(name, aspects, effects, returnDimension, returnVortex, radius);
        return id;
    }

    /** TH: three quarters of the total vis, floored at 32 and capped at 127. */
    public static int radiusFor(Map<AspectId, Integer> aspects) {
        int total = 0;
        for (int amount : aspects.values()) {
            total += Math.max(0, amount);
        }
        int scaled = total * 3 / 4;
        return scaled < 128 ? Math.max(MIN_RADIUS, scaled) : MAX_RADIUS;
    }

    /** TH's colour of a plane: the aspect colours weighted by their share of the total. */
    public static int colourOf(Map<AspectId, Integer> aspects) {
        int total = 0;
        for (int amount : aspects.values()) {
            total += Math.max(0, amount);
        }
        if (total <= 0) {
            return 0;
        }
        int red = 0;
        int green = 0;
        int blue = 0;
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            var definition = AspectApi.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            int colour = definition.color();
            red += (colour >> 16 & 0xFF) * Math.max(0, entry.getValue());
            green += (colour >> 8 & 0xFF) * Math.max(0, entry.getValue());
            blue += (colour & 0xFF) * Math.max(0, entry.getValue());
        }
        return red / total << 16 | green / total << 8 | blue / total;
    }

    private static boolean has(Map<AspectId, Integer> aspects, String path) {
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey().value().getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState tc(String path) {
        Block block = dev.tc4port.thaumcraft.registry.TCBlocks.byId(path).get();
        return block.defaultBlockState();
    }

    /** TH's drawSphere: a one-block-thick shell, drawn with a midpoint raster. */
    private static void shell(ServerLevel level, BlockPos centre, int radius, BlockState state) {
        if (radius < 0) {
            return;
        }
        int x = radius;
        int y = 0;
        int error = 1 - x;
        while (x >= y) {
            circle(level, centre, y, x, error, state);
            y++;
            if (error < 0) {
                error += 2 * y + 1;
            } else {
                x--;
                error += 2 * (y - x + 1);
            }
        }
    }

    /** TH's drawCircle, transcribed: one ring of the shell at vertical offset {@code y1}. */
    private static void circle(ServerLevel level, BlockPos centre, int y1, int radius, int error, BlockState state) {
        int x0 = centre.getX();
        int y0 = centre.getY();
        int z0 = centre.getZ();
        int x = radius;
        int z = 0;
        int radiusError = error;
        while (x >= z) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int band = y0 + sign * y1;
                setFast(level, x0 + x, band, z0 + z, state);
                setFast(level, x0 + z, band, z0 + x, state);
                setFast(level, x0 - x, band, z0 + z, state);
                setFast(level, x0 - z, band, z0 + x, state);
                setFast(level, x0 - x, band, z0 - z, state);
                setFast(level, x0 - z, band, z0 - x, state);
                setFast(level, x0 + x, band, z0 - z, state);
                setFast(level, x0 + z, band, z0 - x, state);

                int side = x0 + sign * y1;
                int top = y0 + x;
                int bottom = y0 - x;
                setFast(level, side, top, z0 + z, state);
                setFast(level, x0 + z, top, z0 + sign * y1, state);
                setFast(level, side, top, z0 - z, state);
                setFast(level, x0 - z, top, z0 + sign * y1, state);
                setFast(level, side, bottom, z0 + z, state);
                setFast(level, x0 + z, bottom, z0 + sign * y1, state);
                setFast(level, side, bottom, z0 - z, state);
                setFast(level, x0 - z, bottom, z0 + sign * y1, state);
            }
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    /** Writes straight into the chunk, as TH did, so a whole shell costs no block updates. */
    private static void setFast(ServerLevel level, int x, int y, int z, BlockState state) {
        PocketPlaneTerrain.setFast(level, x, y, z, state);
    }
}
