package fr.wind_blade.isorropia.common.world;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.common.blocks.BlocksIS;
import fr.wind_blade.isorropia.common.platform.Mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thaumic Horizons' pocket-plane terrain, TH's {@code PocketPlaneData.drawLayers} and {@code drawLayer}. A plane's
 * ground is drawn one layer per resonant aspect, in TH's fixed order (cold, water, taint, cloth, flesh, earth, fire,
 * armour, then dust if nothing matched), each layer carved out of the plane's sphere and limited on top by an octave
 * noise that {@link #calcNoise} bounds.
 *
 * <p>Stage 2c adds the passes TH ran over that ground: {@code drawCaves} (noise-driven tunnels that leave the odd
 * crystal or ore vein behind) and {@code drawPockets} (buried ore veins keyed to the plane's aspects). TH's other
 * terrain passes — {@code drawRavines}, {@code drawClouds}, {@code drawSurfaceFeatures}, {@code drawUndergroundFeatures}
 * and {@code drawLeviathanBones} — are empty stubs upstream, so there is nothing to port for them.</p>
 *
 * <p><b>Worth knowing:</b> TH steps a running {@code level} down between layers, but in {@code drawLayer} that value
 * cancels out of both the top and the bottom of the carved column (the offsets added to the sphere equations subtract
 * straight back off), so the layers do <em>not</em> stack into bands: each present layer repaints the whole sphere and
 * the last matching aspect in the order above is what a visitor sees. This port keeps that behaviour.</p>
 */
public final class PocketPlaneTerrain {
    /** TH capped the number of creatures a single plane spawns. */
    private static final int MAX_CREATURES = 100;
    private static final int OCTAVES = 10;

    /** The 1.7 flower metadata list, in order; TC4 had no metas above it. */
    private static final BlockState[] FLOWERS = {
            Blocks.POPPY.defaultBlockState(),
            Blocks.BLUE_ORCHID.defaultBlockState(),
            Blocks.ALLIUM.defaultBlockState(),
            Blocks.AZURE_BLUET.defaultBlockState(),
            Blocks.RED_TULIP.defaultBlockState(),
            Blocks.ORANGE_TULIP.defaultBlockState(),
            Blocks.WHITE_TULIP.defaultBlockState(),
            Blocks.PINK_TULIP.defaultBlockState(),
            Blocks.OXEYE_DAISY.defaultBlockState(),
    };

    private static final BlockState[] SAPLINGS = {
            Blocks.OAK_SAPLING.defaultBlockState(),
            Blocks.SPRUCE_SAPLING.defaultBlockState(),
            Blocks.BIRCH_SAPLING.defaultBlockState(),
            Blocks.JUNGLE_SAPLING.defaultBlockState(),
            Blocks.ACACIA_SAPLING.defaultBlockState(),
            Blocks.DARK_OAK_SAPLING.defaultBlockState(),
    };

    private static int creatures;

    private PocketPlaneTerrain() {
    }

    /** TH's drawLayers: paints the plane's sphere from the aspects it was made of. */
    public static void drawLayers(ServerLevel level, int radius, BlockPos centre, Map<AspectId, Integer> aspects,
            boolean coldBiome) {
        int layerY = centre.getY();
        boolean drew = false;
        int noise = calcNoise(aspects);
        int life = calcLife(aspects);
        creatures = 0;

        if (amount(aspects, "gelum") > 0) {
            drawLayer(level, radius, centre, Blocks.PACKED_ICE.defaultBlockState(), layerY, 0, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "gelum") * radius);
            drew = true;
        }
        if (amount(aspects, "aqua") > 0) {
            drawLayer(level, radius, centre, Blocks.WATER.defaultBlockState(), layerY, 0, coldBiome, life, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "aqua") * radius / 1.5D);
            drew = true;
        }
        if (amount(aspects, "vitium") > 0) {
            drawLayer(level, radius, centre, tc("tainted_soil"), layerY, noise, coldBiome, life, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "vitium") * radius);
            drew = true;
        }
        if (amount(aspects, "pannus") > 0) {
            drawLayer(level, radius, centre, Blocks.WHITE_WOOL.defaultBlockState(), layerY, noise, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "pannus") * radius);
            drew = true;
        }
        if (amount(aspects, "corpus") > 0) {
            drawLayer(level, radius, centre, tc("tainted_flesh"), layerY, noise, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "pannus") * radius);
            drew = true;
        }
        if (amount(aspects, "terra") > 0) {
            drawLayer(level, radius, centre, Blocks.DIRT.defaultBlockState(), layerY, noise, coldBiome, life, aspects);
            BlockState bedrock = amount(aspects, "ordo") >= amount(aspects, "perditio")
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.COBBLESTONE.defaultBlockState();
            drawLayer(level, radius, centre, bedrock, layerY - 5, noise, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "terra") * radius);
            drew = true;
        }
        if (amount(aspects, "ignis") > 0) {
            drawLayer(level, radius, centre, Blocks.NETHERRACK.defaultBlockState(), layerY, noise, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "ignis") * radius);
            drew = true;
        }
        if (amount(aspects, "tutamen") > 0) {
            drawLayer(level, radius, centre, Blocks.OBSIDIAN.defaultBlockState(), layerY, noise, coldBiome, 0, aspects);
            layerY -= fastFloor(aspectFraction(aspects, "tutamen") * radius);
            drew = true;
        }
        if (!drew) {
            drawLayer(level, radius, centre, BlocksIS.DUST.get().defaultBlockState(), layerY, noise, coldBiome, life,
                    aspects);
        }
    }

    /**
     * TH's drawLayer: carves the part of the plane's sphere that belongs to one layer and paints it, then decorates the
     * top face.
     */
    public static void drawLayer(ServerLevel level, int radius, BlockPos centre, BlockState body, int layerY, int noise,
            boolean coldBiome, int life, Map<AspectId, Integer> aspects) {
        RandomSource random = level.getRandom();
        double[] noiseData = null;
        if (noise != 0) {
            noiseData = octaveNoise(random, centre.getX() - radius, centre.getZ() - radius, 2 * radius, 2 * radius,
                    noise / 25.0D, noise / 25.0D);
        }
        int width = 2 * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int top = 0;
                int index = x + radius + (z + radius) * width;
                if (noise != 0 && noiseData != null && index >= 0 && index < noiseData.length) {
                    top = (int) (noiseData[index] / 8.0D);
                }
                int offsetTop = -layerY + centre.getY();
                int cTop = x * x + z * z + offsetTop * offsetTop - radius * radius;
                double root = offsetTop * offsetTop - cTop;
                if (root < 0) {
                    continue;
                }
                int tMax = offsetTop + (int) Math.sqrt(root);
                int offsetBottom = layerY - centre.getY();
                int cBottom = x * x + z * z + offsetBottom * offsetBottom - radius * radius;
                double bottomRoot = offsetBottom * offsetBottom - cBottom;
                if (bottomRoot < 0) {
                    continue;
                }
                int bMax = offsetBottom + (int) Math.sqrt(bottomRoot);
                if (top > tMax) {
                    top = tMax;
                }
                int bottom = -bMax;
                if (top == bottom) {
                    continue;
                }
                for (int y = -bMax; y <= top; y++) {
                    if (layerY + y <= level.getMinBuildHeight()) {
                        continue;
                    }
                    int worldX = x + centre.getX();
                    int worldY = y + layerY;
                    int worldZ = z + centre.getZ();
                    if (y == top) {
                        setFast(level, worldX, worldY, worldZ, body);
                        decorate(level, worldX, worldY, worldZ, body, life, coldBiome, aspects);
                    } else {
                        setFast(level, worldX, worldY, worldZ, body);
                    }
                }
            }
        }
    }

    /**
     * TH's {@code drawCaves} then {@code drawPockets}, in the order {@code generatePocketPlane} ran them. The integrator
     * calls this straight after {@link #drawLayers}, so the caverns and veins are cut into the layered ground before the
     * aspect rings and the void bubble are drawn over it.
     */
    public static void drawCavesAndPockets(ServerLevel level, int radius, BlockPos centre,
            Map<AspectId, Integer> aspects) {
        int noise = calcNoise(aspects);
        drawCaves(level, radius, centre, aspects, noise);
        drawPockets(level, radius, centre, aspects, noise);
    }

    /** TH's drawCaves: hollows noise-driven caverns out of the plane's solid ground and veins their walls. */
    private static void drawCaves(ServerLevel level, int radius, BlockPos centre, Map<AspectId, Integer> aspects,
            int noise) {
        RandomSource random = level.getRandom();

        // How solid the sphere stays: air and void hollow it out, order and craft pack it back, flight and trap loosen it.
        float solidity = 0.5F;
        solidity -= aspectFraction(aspects, "aer") * 0.5F;
        solidity += aspectFraction(aspects, "ordo") * 0.25F;
        solidity -= aspectFraction(aspects, "vacuos");
        solidity -= aspectFraction(aspects, "volatus") * 0.5F;
        solidity += aspectFraction(aspects, "fabrico") * 0.5F;
        solidity -= aspectFraction(aspects, "vinculum") * 0.5F;
        solidity = Math.max(0.0F, Math.min(1.0F, solidity));
        int numCaves = (int) (radius * radius * radius / 10000 * (solidity + 0.1F));

        // What a cavern may be laced with. TH collected these into a HashMap and rolled against it by weight; the port
        // keeps TH's insertion order, since a HashMap's order is not reproducible and the weights are what matter.
        Map<AspectId, Double> dressing = new LinkedHashMap<>();
        for (String path : CAVE_DRESSING) {
            dredge(dressing, aspects, path);
        }

        for (int i = 0; i < numCaves; i++) {
            int xSize = random.nextInt(10) + 10;
            int zSize = random.nextInt(10) + 10;
            int xOffset = random.nextInt((int) (radius * 1.5F)) - (int) (radius * 0.75F);
            int yOffset = -random.nextInt((int) (radius * 0.5F));
            int zOffset = random.nextInt((int) (radius * 1.5F)) - (int) (radius * 0.75F);
            drawACave(level, centre, noise, xSize, zSize, xOffset, yOffset, zOffset, dressing);
        }
    }

    /**
     * TH's drawACave: over a box of {@code xSize} by sixteen by {@code zSize}, everything the octave noise puts above
     * average is carved to air; where that cell sits on solid ground there is a one-in-fifty chance the roll turns
     * instead into a vein. TH anchored the box with {@code - xSize + xOffset}, which nudges it back by its own width.
     */
    private static void drawACave(ServerLevel level, BlockPos centre, int noise, int xSize, int zSize, int xOffset,
            int yOffset, int zOffset, Map<AspectId, Double> dressing) {
        RandomSource random = level.getRandom();
        int baseX = centre.getX() - xSize + xOffset;
        int baseY = centre.getY() + yOffset;
        int baseZ = centre.getZ() - zSize + zOffset;
        double[] noiseData = octaveNoise3(random, baseX, baseY, baseZ, xSize, 16, zSize, noise / 10.0D, noise / 25.0D,
                noise / 10.0D);
        int avg = getAvg(noiseData);

        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                for (int y = 0; y < 16; y++) {
                    if (noiseData[x + xSize * z + y * xSize * zSize] <= avg) {
                        continue;
                    }
                    int wx = baseX + x;
                    int wy = baseY + y;
                    int wz = baseZ + z;
                    BlockState here = level.getBlockState(new BlockPos(wx, wy, wz));
                    // TH never touches a water block (1.7's flowing water; the port has one water block for both).
                    if (here.is(Blocks.WATER)) {
                        continue;
                    }
                    if (!level.getBlockState(new BlockPos(wx, wy - 1, wz)).isAir() && random.nextInt(50) == 49) {
                        AspectId which = pickWeighted(dressing, random);
                        // A miss, or a ceiling of already-carved air, leaves the cell as it was: TH only dresses stone.
                        if (which != null && !here.isAir()) {
                            setFast(level, wx, wy, wz, veinBlock(which, random));
                        }
                    } else {
                        setFast(level, wx, wy, wz, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    /** TH's drawPockets: scatters buried ore veins through the lower half of the plane, one aspect's ore per pocket. */
    private static void drawPockets(ServerLevel level, int radius, BlockPos centre, Map<AspectId, Integer> aspects,
            int noise) {
        RandomSource random = level.getRandom();
        int numPockets = radius * radius * radius / 200;
        Map<AspectId, Double> probabilities = new LinkedHashMap<>();
        for (String path : POCKET_DRESSING) {
            dredge(probabilities, aspects, path);
        }

        // TH hoisted the block out of the loop and only replaced it when a roll matched an aspect, so a roll that misses
        // leaves a pocket of whatever the last matching pocket was.
        BlockState block = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < numPockets; i++) {
            AspectId which = pickWeighted(probabilities, random);
            if (which != null) {
                BlockState chosen = pocketVein(which.value().getPath(), random);
                if (chosen != null) {
                    block = chosen;
                }
            }
            int xSize = random.nextInt(3) + 1;
            int zSize = random.nextInt(3) + 1;
            int xOffset = random.nextInt(radius * 2) - radius;
            int yOffset = -random.nextInt(radius);
            int zOffset = random.nextInt(radius * 2) - radius;
            drawAPocket(level, centre, noise, block, xSize, zSize, xOffset, yOffset, zOffset);
        }
    }

    /**
     * TH's drawAPocket: a two-block slab of ore tucked among the plane's solid ground. TH built a top and a bottom slice
     * from the same generator with the same arguments, so the two are identical and one slice stands in for both.
     */
    private static void drawAPocket(ServerLevel level, BlockPos centre, int noise, BlockState block, int xSize,
            int zSize, int xOffset, int yOffset, int zOffset) {
        RandomSource random = level.getRandom();
        int baseX = centre.getX() - xSize + xOffset;
        int baseY = centre.getY() + yOffset;
        int baseZ = centre.getZ() - zSize + zOffset;
        double[] slice = octaveNoise3(random, baseX, baseY, baseZ, xSize, 1, zSize, noise / 50.0D, noise / 25.0D,
                noise / 50.0D);

        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                int height = (int) (slice[x + z * xSize] / 16.0D);
                for (int y = height - 1; y < height + 1; y++) {
                    int wx = baseX + x;
                    int wy = baseY + y;
                    int wz = baseZ + z;
                    BlockState here = level.getBlockState(new BlockPos(wx, wy, wz));
                    // A vein only replaces solid, non-water ground: a pocket is ore inside the terrain, not a hole.
                    if (!here.isAir() && !here.is(Blocks.WATER)) {
                        setFast(level, wx, wy, wz, block);
                    }
                }
            }
        }
    }

    /** TH's drawCaves dressing table, in the order TH inserted it. */
    private static final String[] CAVE_DRESSING = {
            "terra", "ignis", "ordo", "aer", "aqua", "perditio", "metallum", "mortuus", "exanimis", "sensus", "lucrum",
    };

    /** TH's drawPockets probability table, in the order TH inserted it. */
    private static final String[] POCKET_DRESSING = {
            "terra", "ignis", "ordo", "aer", "aqua", "perditio", "metallum", "vitreus", "spiritus", "praecantatio",
            "auram", "potentia", "arbor", "corpus", "sensus", "perfodio", "instrumentum", "lucrum", "vitium", "venenum",
            "permutatio", "cognitio", "humanus", "fames", "fabrico",
    };

    /** Adds an aspect to a weighted table, but only when the plane actually holds it, as TH checked. */
    private static void dredge(Map<AspectId, Double> table, Map<AspectId, Integer> aspects, String path) {
        if (amount(aspects, path) <= 0) {
            return;
        }
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            if (entry.getKey().value().getPath().equals(path)) {
                table.put(entry.getKey(), aspectFraction(aspects, path));
                return;
            }
        }
    }

    /**
     * TH's weighted roll: one {@code nextFloat}, then walk the table subtracting each weight until it fits. Returns null
     * when the roll lands past every weight, which TH read as "no dressing at all".
     */
    private static AspectId pickWeighted(Map<AspectId, Double> table, RandomSource random) {
        float which = random.nextFloat();
        for (Map.Entry<AspectId, Double> entry : table.entrySet()) {
            float weight = entry.getValue().floatValue();
            if (which <= weight) {
                return entry.getKey();
            }
            which -= weight;
        }
        return null;
    }

    /** TH's drawACave dressing: six aspect crystals, a skull, lapis, gold or iron. */
    private static BlockState veinBlock(AspectId aspect, RandomSource random) {
        return switch (aspect.value().getPath()) {
            case "terra" -> tc("earth_crystal_cluster");
            case "aer" -> tc("air_crystal_cluster");
            case "aqua" -> tc("water_crystal_cluster");
            case "ignis" -> tc("fire_crystal_cluster");
            case "ordo" -> tc("order_crystal_cluster");
            case "perditio" -> tc("entropy_crystal_cluster");
            case "mortuus", "exanimis" -> Blocks.SKELETON_SKULL.defaultBlockState();
            case "sensus" -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case "lucrum" -> random.nextBoolean()
                    ? Blocks.GOLD_BLOCK.defaultBlockState()
                    : Blocks.EMERALD_BLOCK.defaultBlockState();
            case "metallum" -> random.nextBoolean()
                    ? Blocks.GOLD_BLOCK.defaultBlockState()
                    : Blocks.IRON_BLOCK.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    /** TH's drawPockets ore table: the aspect's ore, or null when the aspect has none and the last pocket repeats. */
    private static BlockState pocketVein(String path, RandomSource random) {
        return switch (path) {
            case "terra" -> infusedOre(4);
            case "aer" -> infusedOre(1);
            case "aqua" -> infusedOre(3);
            case "ignis" -> random.nextBoolean() ? infusedOre(2) : Blocks.LAVA.defaultBlockState();
            case "ordo" -> infusedOre(5);
            case "perditio" -> infusedOre(6);
            case "metallum" -> metalOre(random);
            case "vitreus" -> Blocks.QUARTZ_BLOCK.defaultBlockState();
            case "spiritus" -> Blocks.SOUL_SAND.defaultBlockState();
            case "praecantatio", "auram" -> infusedOre(random.nextInt(6) + 1);
            case "potentia" -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case "arbor" -> random.nextBoolean() ? Blocks.OAK_LOG.defaultBlockState() : infusedOre(7);
            case "corpus" -> tc("tainted_flesh");
            case "sensus" -> Blocks.LAPIS_ORE.defaultBlockState();
            case "perfodio", "instrumentum" -> mineOre(random);
            case "lucrum" -> greedOre(random);
            case "vitium" -> tc("crusted_taint");
            case "venenum" -> liquidDeath();
            case "permutatio" -> infusedOre(0);
            case "cognitio" -> random.nextBoolean() ? Blocks.BOOKSHELF.defaultBlockState() : brainBlock();
            case "humanus" -> switch (random.nextInt(3)) {
                case 0 -> Blocks.BOOKSHELF.defaultBlockState();
                case 1 -> brainBlock();
                default -> tc("tainted_flesh");
            };
            case "fames" -> tc("tainted_flesh");
            case "fabrico" -> Blocks.CRAFTING_TABLE.defaultBlockState();
            default -> null;
        };
    }

    /**
     * TH's {@code blockCustomOre} metadata, recovered from the legacy tile names: 0 cinnabar, 1..6 the six infused
     * stones in primal order, 7 amber. TC4R splits it into one block per ore.
     */
    private static BlockState infusedOre(int metadata) {
        return switch (metadata) {
            case 0 -> tc("cinnabar_ore");
            case 1 -> tc("air_infused_stone");
            case 2 -> tc("fire_infused_stone");
            case 3 -> tc("water_infused_stone");
            case 4 -> tc("earth_infused_stone");
            case 5 -> tc("order_infused_stone");
            case 6 -> tc("entropy_infused_stone");
            default -> tc("amber_bearing_stone");
        };
    }

    /** TH's metal branch: half iron, two fifths gold, the rest cinnabar. */
    private static BlockState metalOre(RandomSource random) {
        if (random.nextFloat() <= 0.5F) {
            return Blocks.IRON_ORE.defaultBlockState();
        }
        if (random.nextFloat() <= 0.8F) {
            return Blocks.GOLD_ORE.defaultBlockState();
        }
        return infusedOre(0);
    }

    /** TH's mine and tool branch: a weighted spread of vanilla ores, then a random Thaumcraft one. */
    private static BlockState mineOre(RandomSource random) {
        float which = random.nextFloat();
        if (which < 0.4F) {
            return Blocks.IRON_ORE.defaultBlockState();
        }
        if (which < 0.5F) {
            return Blocks.GOLD_ORE.defaultBlockState();
        }
        if (which < 0.6F) {
            return Blocks.DIAMOND_ORE.defaultBlockState();
        }
        if (which < 0.7F) {
            return Blocks.EMERALD_ORE.defaultBlockState();
        }
        if (which < 0.8F) {
            return Blocks.LAPIS_ORE.defaultBlockState();
        }
        return infusedOre(random.nextInt(8));
    }

    /** TH's greed branch: gold, diamond, emerald or lapis. */
    private static BlockState greedOre(RandomSource random) {
        float which = random.nextFloat();
        if (which < 0.4F) {
            return Blocks.GOLD_ORE.defaultBlockState();
        }
        if (which < 0.6F) {
            return Blocks.DIAMOND_ORE.defaultBlockState();
        }
        if (which < 0.8F) {
            return Blocks.EMERALD_ORE.defaultBlockState();
        }
        return Blocks.LAPIS_ORE.defaultBlockState();
    }

    /**
     * TH laid its own brain block here, a taint-material block that dropped zombie brains. The port has no brain block
     * and cannot add one (the block's model, texture and lang belong to another module), so it uses the tainted flesh
     * that TH's brain reused the same material as.
     */
    private static BlockState brainBlock() {
        return tc("tainted_flesh");
    }

    /**
     * TH set {@code blockFluidDeath} at metadata 16 for a poison pocket. That is past the end of TC4's finite-fluid
     * range, so the port places the block at its fullest level instead of guessing at a clamped metadata.
     */
    private static BlockState liquidDeath() {
        return dev.tc4port.thaumcraft.registry.TCBlocks.LIQUID_DEATH.get().overfullState();
    }

    /** TH's getAvg: each value truncated to a long before the sum, then the whole divided down and truncated again. */
    private static int getAvg(double[] array) {
        long total = 0L;
        for (double value : array) {
            total += (long) value;
        }
        total /= array.length;
        return (int) total;
    }

    /** TH's surface dressing: grass and crops on dirt, ice and lilies on water, pillars, mobs and so on. */
    private static void decorate(ServerLevel level, int x, int y, int z, BlockState body, int life, boolean coldBiome,
            Map<AspectId, Integer> aspects) {
        RandomSource random = level.getRandom();
        boolean airAbove = level.getBlockState(new BlockPos(x, y + 1, z)).isAir();

        if (body.is(Blocks.WATER)) {
            if (coldBiome) {
                setFast(level, x, y, z, Blocks.ICE.defaultBlockState());
            } else {
                setFast(level, x, y, z, Blocks.WATER.defaultBlockState());
                if ((life > 40 || amount(aspects, "bestia") > 0) && random.nextInt(100) > 98
                        && creatures < MAX_CREATURES) {
                    spawn(level, EntityType.SQUID, x, y, z);
                }
                if (life > 0 && random.nextInt(100) > 98) {
                    setFast(level, x, y + 1, z, Blocks.LILY_PAD.defaultBlockState());
                }
            }
        } else if (body.is(Blocks.DIRT) && life > 0 && airAbove) {
            setFast(level, x, y, z, Blocks.GRASS_BLOCK.defaultBlockState());
            if (life >= 10) {
                if ((life >= 20 || amount(aspects, "messis") > 0 || amount(aspects, "meto") > 0)
                        && random.nextInt(100) > 97) {
                    crop(level, random, x, y, z);
                } else if ((life >= 15 || amount(aspects, "arbor") > 0) && random.nextInt(100) > 97) {
                    sapling(level, random, x, y, z, aspects);
                } else if ((life >= 10 || amount(aspects, "herba") > 0) && random.nextInt(100) > 94) {
                    int pick = random.nextInt(18);
                    if (pick <= 15) {
                        // TC4's red_flower only had nine identities; higher rolls fold back onto them.
                        setFast(level, x, y + 1, z, FLOWERS[pick % FLOWERS.length]);
                    } else {
                        setFast(level, x, y + 1, z, pick == 16
                                ? Blocks.DEAD_BUSH.defaultBlockState()
                                : Blocks.GRASS.defaultBlockState());
                    }
                }
            } else {
                setFast(level, x, y, z, body);
            }
        } else if (body.is(Blocks.SAND)) {
            if (life > 10) {
                if (life >= 20 && random.nextInt(100) > 98) {
                    setFast(level, x, y + 1, z, tc("cinderpearl"));
                } else if (random.nextInt(100) > 97) {
                    setFast(level, x, y + 1, z, Blocks.CACTUS.defaultBlockState());
                }
            }
        }

        // Eldritch planes raise the occasional obsidian shrine; craft planes leave an arcane pedestal.
        if (amount(aspects, "alienis") > 0 && random.nextInt(200) > 198) {
            setFast(level, x, y, z, tc("obsidian_tile"));
            setFast(level, x, y + 1, z, tc("obsidian_totem"));
            setFast(level, x, y + 2, z, tc("obsidian_totem"));
            setFast(level, x, y + 3, z, tc("obsidian_totem"));
        }
        if (amount(aspects, "fabrico") > 0 && random.nextInt(200) > 198) {
            setFast(level, x, y + 1, z, tc("arcane_pedestal"));
        }
        if (amount(aspects, "vitium") > 0 && random.nextInt(100) > 98) {
            setFast(level, x, y, z, tc("fibrous_taint"));
        }

        // Beasts roam a living plane.
        if ((life >= 50 || amount(aspects, "bestia") > 0) && random.nextInt(100) > 98
                && creatures < MAX_CREATURES) {
            EntityType<? extends Animal> type = switch (random.nextInt(7)) {
                case 1 -> EntityType.PIG;
                case 2 -> EntityType.CHICKEN;
                case 3 -> EntityType.SHEEP;
                case 4 -> EntityType.HORSE;
                case 5 -> EntityType.OCELOT;
                case 6 -> EntityType.WOLF;
                default -> EntityType.COW;
            };
            spawn(level, type, x, y, z);
        }
        if (amount(aspects, "limus") > 0 && random.nextInt(100) > 98 && creatures < MAX_CREATURES) {
            spawn(level, EntityType.SLIME, x, y, z);
        }

        if (coldBiome && body.isSolidRender(level, new BlockPos(x, y, z))) {
            setFast(level, x, y + 1, z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
        }
    }

    /** TH's ten-way crop draw on a grass top. */
    private static void crop(ServerLevel level, RandomSource random, int x, int y, int z) {
        switch (random.nextInt(10)) {
            case 0 -> {
                setFast(level, x, y, z, Blocks.FARMLAND.defaultBlockState());
                setFast(level, x, y + 1, z, Blocks.CARROTS.defaultBlockState());
            }
            case 1 -> {
                setFast(level, x, y, z, Blocks.FARMLAND.defaultBlockState());
                setFast(level, x, y + 1, z, Blocks.POTATOES.defaultBlockState());
            }
            case 2 -> {
                setFast(level, x, y, z, Blocks.FARMLAND.defaultBlockState());
                setFast(level, x, y + 1, z, Blocks.WHEAT.defaultBlockState());
            }
            case 3 -> setFast(level, x, y + 1, z, Blocks.MELON.defaultBlockState());
            case 4 -> setFast(level, x, y + 1, z, Blocks.PUMPKIN.defaultBlockState());
            case 5 -> setFast(level, x, y + 1, z, Blocks.SUGAR_CANE.defaultBlockState());
            case 6 -> {
                setFast(level, x, y, z, Blocks.MYCELIUM.defaultBlockState());
                setFast(level, x, y + 1, z, Blocks.RED_MUSHROOM.defaultBlockState());
            }
            case 7 -> {
                setFast(level, x, y, z, Blocks.MYCELIUM.defaultBlockState());
                setFast(level, x, y + 1, z, Blocks.BROWN_MUSHROOM.defaultBlockState());
            }
            case 8 -> setFast(level, x, y + 1, z, tc("shimmerleaf"));
            default -> setFast(level, x, y + 1, z, tc("vishroom"));
        }
    }

    /** TH's eight-way sapling draw, including the two Thaumcraft saplings. */
    private static void sapling(ServerLevel level, RandomSource random, int x, int y, int z,
            Map<AspectId, Integer> aspects) {
        switch (random.nextInt(10)) {
            case 0, 1, 2, 3, 4, 5 -> setFast(level, x, y + 1, z, SAPLINGS[random.nextInt(SAPLINGS.length)]);
            case 6 -> {
                if ((amount(aspects, "praecantatio") > 10 || amount(aspects, "auram") > 5)
                        && random.nextInt(4) == 0) {
                    setFast(level, x, y + 1, z, tc("silverwood_sapling"));
                }
            }
            case 7 -> {
                if (random.nextInt(3) == 0) {
                    setFast(level, x, y + 1, z, tc("greatwood_sapling"));
                }
            }
            default -> {
                // TH's remaining two rolls placed nothing.
            }
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Entity> type, int x, int y, int z) {
        Entity entity = type.create(level);
        if (entity == null) {
            return;
        }
        entity.moveTo(x + 0.5, y + 1.0, z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            Mobs.finalizeSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.STRUCTURE);
        }
        if (level.addFreshEntity(entity)) {
            creatures++;
        }
    }

    /** TH's calcNoise: how badly a plane's terrain breaks up, 0 (orderly) to 100 (chaotic). */
    public static int calcNoise(Map<AspectId, Integer> aspects) {
        double noise = 50.0D;
        noise -= aspectFraction(aspects, "ordo") * 100.0D;
        noise += aspectFraction(aspects, "perditio") * 100.0D;
        noise += aspectFraction(aspects, "motus") * 50.0D;
        noise += aspectFraction(aspects, "permutatio") * 100.0D;
        noise += aspectFraction(aspects, "volatus") * 50.0D;
        noise -= aspectFraction(aspects, "iter") * 100.0D;
        return fastFloor(Math.max(0.0D, Math.min(noise, 100.0D)));
    }

    /** TH's calcLife: how alive a plane is, 0 to 100. */
    public static int calcLife(Map<AspectId, Integer> aspects) {
        double life = 0.0D;
        life += aspectFraction(aspects, "lux") * 25.0D;
        life += aspectFraction(aspects, "tempestas") * 25.0D;
        life += aspectFraction(aspects, "victus") * 150.0D;
        life -= aspectFraction(aspects, "venenum") * 75.0D;
        life -= aspectFraction(aspects, "mortuus") * 100.0D;
        life -= aspectFraction(aspects, "tenebrae") * 25.0D;
        life += aspectFraction(aspects, "spiritus") * 50.0D;
        life += aspectFraction(aspects, "sano") * 150.0D;
        life += aspectFraction(aspects, "auram") * 50.0D;
        life += aspectFraction(aspects, "limus") * 50.0D;
        life += aspectFraction(aspects, "herba") * 100.0D;
        life += aspectFraction(aspects, "arbor") * 100.0D;
        life += aspectFraction(aspects, "bestia") * 100.0D;
        life += aspectFraction(aspects, "corpus") * 50.0D;
        life -= aspectFraction(aspects, "exanimis") * 50.0D;
        life += aspectFraction(aspects, "cognitio") * 25.0D;
        life += aspectFraction(aspects, "sensus") * 25.0D;
        life += aspectFraction(aspects, "humanus") * 25.0D;
        life += aspectFraction(aspects, "messis") * 50.0D;
        life += aspectFraction(aspects, "meto") * 25.0D;
        life -= aspectFraction(aspects, "telum") * 25.0D;
        life -= aspectFraction(aspects, "fames") * 25.0D;
        life += aspectFraction(aspects, "pannus") * 25.0D;
        return fastFloor(Math.max(0.0D, Math.min(life, 100.0D)));
    }

    public static double aspectFraction(Map<AspectId, Integer> aspects, String path) {
        int total = total(aspects);
        return total == 0 ? 0.0D : (double) amount(aspects, path) / total;
    }

    private static int total(Map<AspectId, Integer> aspects) {
        int total = 0;
        for (int value : aspects.values()) {
            total += Math.max(0, value);
        }
        return total;
    }

    private static int amount(Map<AspectId, Integer> aspects, String path) {
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            if (entry.getKey().value().getPath().equals(path)) {
                return Math.max(0, entry.getValue());
            }
        }
        return 0;
    }

    /** TH's fastFloor: truncation toward zero is wrong for negatives, so it floors properly. */
    public static int fastFloor(double value) {
        int truncated = (int) value;
        return truncated > value ? truncated - 1 : truncated;
    }

    private static BlockState tc(String path) {
        Block block = dev.tc4port.thaumcraft.registry.TCBlocks.byId(path).get();
        return block.defaultBlockState();
    }

    /**
     * A stand-in for 1.7's {@code NoiseGeneratorOctaves} over a single-height slice: ten octaves of Perlin noise with
     * halving amplitude. TC4R cannot reproduce TC4's permutation, so the exact terrain silhouette will differ, but the
     * scale, the octave count and the amplitude falloff match.
     */
    private static double[] octaveNoise(RandomSource random, int xOffset, int zOffset, int xSize, int zSize,
            double xScale, double zScale) {
        return octaveNoise3(random, xOffset, 0, zOffset, xSize, 1, zSize, xScale, 0.0D, zScale);
    }

    /**
     * The same stand-in over a box: the cave and pocket passes sample a slab (and the pocket pass a single slice of one),
     * so they share this generator rather than a second noise implementation. Indexed x, then z, then y — the order TH
     * read {@code generateNoiseOctaves}' output in.
     */
    private static double[] octaveNoise3(RandomSource random, int xOffset, int yOffset, int zOffset, int xSize,
            int ySize, int zSize, double xScale, double yScale, double zScale) {
        Perlin[] octaves = new Perlin[OCTAVES];
        for (int i = 0; i < OCTAVES; i++) {
            octaves[i] = new Perlin(random);
        }
        double[] out = new double[xSize * ySize * zSize];
        double amplitude = 1.0D;
        for (int octave = 0; octave < OCTAVES; octave++) {
            double baseX = xOffset * amplitude * xScale;
            double baseY = yOffset * amplitude * yScale;
            double baseZ = zOffset * amplitude * zScale;
            for (int x = 0; x < xSize; x++) {
                for (int z = 0; z < zSize; z++) {
                    for (int y = 0; y < ySize; y++) {
                        out[x + z * xSize + y * xSize * zSize] += octaves[octave].noise(baseX + x * xScale * amplitude,
                                baseY + y * yScale * amplitude, baseZ + z * zScale * amplitude) * amplitude;
                    }
                }
            }
            amplitude /= 2.0D;
        }
        return out;
    }

    /** Classic three-dimensional Perlin noise, seeded from the level's random source. */
    private static final class Perlin {
        private final int[] perm = new int[512];

        Perlin(RandomSource random) {
            int[] source = new int[256];
            for (int i = 0; i < 256; i++) {
                source[i] = i;
            }
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = source[i];
                source[i] = source[j];
                source[j] = swap;
            }
            for (int i = 0; i < 512; i++) {
                this.perm[i] = source[i & 255];
            }
        }

        double noise(double x, double y, double z) {
            int xi = floor(x) & 255;
            int yi = floor(y) & 255;
            int zi = floor(z) & 255;
            double xf = x - floor(x);
            double yf = y - floor(y);
            double zf = z - floor(z);
            double u = fade(xf);
            double v = fade(yf);
            double w = fade(zf);
            int aaa = this.perm[this.perm[this.perm[xi] + yi] + zi];
            int aba = this.perm[this.perm[this.perm[xi] + yi + 1] + zi];
            int aab = this.perm[this.perm[this.perm[xi] + yi] + zi + 1];
            int abb = this.perm[this.perm[this.perm[xi] + yi + 1] + zi + 1];
            int baa = this.perm[this.perm[this.perm[xi + 1] + yi] + zi];
            int bba = this.perm[this.perm[this.perm[xi + 1] + yi + 1] + zi];
            int bab = this.perm[this.perm[this.perm[xi + 1] + yi] + zi + 1];
            int bbb = this.perm[this.perm[this.perm[xi + 1] + yi + 1] + zi + 1];
            double first = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf));
            double second = lerp(u, grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf));
            double lower = lerp(v, first, second);
            double third = lerp(u, grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1));
            double fourth = lerp(u, grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1));
            double upper = lerp(v, third, fourth);
            return lerp(w, lower, upper);
        }

        private static int floor(double value) {
            int truncated = (int) value;
            return truncated > value ? truncated - 1 : truncated;
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        }

        private static double lerp(double t, double a, double b) {
            return a + t * (b - a);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }

    /** Writes straight into the chunk, as TH did, so a whole sphere costs no block updates. */
    static void setFast(ServerLevel level, int x, int y, int z, BlockState state) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        chunk.setBlockState(new BlockPos(x, y, z), state, false);
    }
}
