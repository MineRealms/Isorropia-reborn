package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thaumic Horizons' {@code PocketPlaneData.setBiome}: a plane's ground is tinted by picking a biome from the aspects it
 * was built out of, and the terrain dressing then reads that biome's temperature. TH returned one of TC4's biomes
 * (tainted, eerie, magical forest, or - when nothing resonant was present - a vanilla biome chosen from a temperature
 * and moisture derived from the aspects) and wrote it into every column of the plane's square.
 *
 * <p>Modern Minecraft registers biomes through the datapack, so a biome cannot be conjured from aspects at runtime.
 * This port keeps a small fixed set under {@code data/isorropia/worldgen/biome/} and reproduces TH's chooser over it:</p>
 *
 * <ul>
 *   <li>{@code vitium} (TH TAINT) &rarr; {@link #POCKET_PLANE_TAINTED}, TH's tainted land;</li>
 *   <li>{@code alienis} / {@code exanimis} (TH ELDRITCH / UNDEAD) &rarr; also {@link #POCKET_PLANE_TAINTED}, but see
 *       the note below;</li>
 *   <li>{@code praecantatio} / {@code auram} (TH MAGIC / AURA) &rarr; {@link #POCKET_PLANE_MAGICAL};</li>
 *   <li>nothing resonant &rarr; {@link #POCKET_PLANE}, standing in for TH's climate tree.</li>
 * </ul>
 *
 * <p><b>Deviations from TH, kept deliberately small:</b></p>
 * <ul>
 *   <li>TH had three <em>distinct</em> aspect biomes (taint, eerie, magical forest); this port folds eerie into the
 *       tainted variant, so the aspect set is one biome that covers TH's two "blighted" picks. Eerie is a spooky
 *       overworld biome and tainted land is TC4's purple corruption, so the merge is a compromise, not an identity.</li>
 *   <li>TH's climate branch returned six different vanilla biomes; all six collapse onto the single neutral
 *       {@link #POCKET_PLANE}.</li>
 *   <li>TH's biomes carried their own temperature, which drove the terrain's cold dressing; fixed datapack biomes
 *       cannot vary per plane, so {@link #isCold} reproduces TH's <em>decision</em> instead (see its javadoc).</li>
 * </ul>
 *
 * <p>1.7 biomes were one value per block column. Modern biomes are stored per 4&times;4&times;4 cell, so the written
 * region is rounded out to quart cells; a plane's square of side {@code 2 * radius + 1} therefore gains up to three
 * blocks of biome at each edge.</p>
 */
public final class PocketPlaneBiomes {
    /** TH's climate fallback: a neutral pocket biome, standing in for plains/forest/desert/... */
    public static final ResourceKey<Biome> POCKET_PLANE = key("pocket_plane");
    /** TH's {@code biomeTaint} branch, also standing in for the eerie branch. */
    public static final ResourceKey<Biome> POCKET_PLANE_TAINTED = key("pocket_plane_tainted");
    /** TH's {@code biomeMagicalForest} branch. */
    public static final ResourceKey<Biome> POCKET_PLANE_MAGICAL = key("pocket_plane_magical");

    private PocketPlaneBiomes() {
    }

    /**
     * TH's {@code setBiome}, reduced to the fixed pocket biomes. TH tested the aspects in the order taint, eerie,
     * magical forest, climate; the first hit wins.
     */
    public static ResourceKey<Biome> choose(Map<AspectId, Integer> aspects) {
        if (amount(aspects, "vitium") > 0) {
            return POCKET_PLANE_TAINTED;
        }
        if (amount(aspects, "alienis") > 0 || amount(aspects, "exanimis") > 0) {
            return POCKET_PLANE_TAINTED;
        }
        if (amount(aspects, "praecantatio") > 0 || amount(aspects, "auram") > 0) {
            return POCKET_PLANE_MAGICAL;
        }
        return POCKET_PLANE;
    }

    /**
     * Reproduces the temperature side of TH's climate branch, for the terrain's cold dressing. TH gave each chosen
     * vanilla biome its own temperature and {@code drawLayer} froze water and dusted snow when it fell below 0.15;
     * only the icePlains branch (aspect temperature at or below 0.2) was cold. The taint / eerie / magical forest
     * biomes are all warm enough not to freeze, so they report false here, exactly as TH's terrain saw them.
     *
     * <p>The integrator can use this in place of {@code PocketPlaneBuilder}'s current
     * {@code pocket.getBiome(centre).value().getBaseTemperature() < 0.15F}, which - against a fixed biome - can no
     * longer follow a plane's aspects.</p>
     */
    public static boolean isCold(Map<AspectId, Integer> aspects) {
        if (amount(aspects, "vitium") > 0 || amount(aspects, "alienis") > 0 || amount(aspects, "exanimis") > 0
                || amount(aspects, "praecantatio") > 0 || amount(aspects, "auram") > 0) {
            return false;
        }
        return climateTemperature(aspects) <= 0.2F;
    }

    /**
     * TH's climate temperature, 0 (frozen) to 1 (scorching), clamped. Only its cold end survives in the port because
     * every other climate result collapses onto {@link #POCKET_PLANE}; it is kept whole so the derivation stays
     * readable against {@code PocketPlaneData.setBiome}.
     */
    static float climateTemperature(Map<AspectId, Integer> aspects) {
        float temp = 0.5F;
        temp += fraction(aspects, "ignis");
        temp -= fraction(aspects, "aqua") * 0.5F;
        temp -= fraction(aspects, "ordo") * 0.5F;
        temp -= fraction(aspects, "vacuos") * 0.5F;
        temp += fraction(aspects, "lux") * 0.5F;
        temp -= fraction(aspects, "tempestas") * 0.25F;
        temp += fraction(aspects, "motus") * 0.5F;
        temp -= fraction(aspects, "gelum");
        temp += fraction(aspects, "potentia") * 0.5F;
        temp -= fraction(aspects, "tenebrae") * 0.25F;
        if (temp < 0.0F) {
            return 0.0F;
        }
        return Math.min(temp, 1.0F);
    }

    /**
     * TH's {@code setBiome} loop, plus the modern equivalent of {@code Utils.setBiomeAt}. TH repainted every column of
     * the plane's square; here the whole 4&times;4&times;4 cell of each column is repainted, over every chunk that has
     * already been loaded (the chunks the terrain stamped - the void corners are left alone). Returns the number of
     * chunks rewritten.
     */
    public static int apply(ServerLevel level, BlockPos centre, int radius, ResourceKey<Biome> biome) {
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> replacement = registry.getHolderOrThrow(biome);
        int minX = centre.getX() - radius;
        int maxX = centre.getX() + radius;
        int minZ = centre.getZ() - radius;
        int maxZ = centre.getZ() + radius;

        List<ChunkAccess> changed = new ArrayList<>();
        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX);
                chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ);
                    chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                chunk.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) -> inSquare(quartX, quartZ, minX, maxX,
                        minZ, maxZ) ? replacement : chunk.getNoiseBiome(quartX, quartY, quartZ),
                        level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
                changed.add(chunk);
            }
        }
        if (!changed.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
        }
        return changed.size();
    }

    private static boolean inSquare(int quartX, int quartZ, int minX, int maxX, int minZ, int maxZ) {
        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        return blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ;
    }

    private static double fraction(Map<AspectId, Integer> aspects, String path) {
        int total = 0;
        for (int value : aspects.values()) {
            total += Math.max(0, value);
        }
        return total == 0 ? 0.0D : (double) amount(aspects, path) / total;
    }

    private static int amount(Map<AspectId, Integer> aspects, String path) {
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            if (entry.getKey().value().getPath().equals(path)) {
                return Math.max(0, entry.getValue());
            }
        }
        return 0;
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, Isorropia.id(path));
    }
}
