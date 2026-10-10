package fr.wind_blade.isorropia.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DuoWeightedRandomTest {
    private static final List<String> ORES = List.of("copper", "gold", "iron");

    @Test
    void withoutMainEveryEntryWeighsTheSame() {
        assertEquals(3.0D, DuoWeightedRandom.totalWeight(ORES, null, 1.0D, 5.0D));
        assertEquals("copper", DuoWeightedRandom.pick(ORES, null, 1.0D, 5.0D, () -> 0.0D));
        assertEquals("gold", DuoWeightedRandom.pick(ORES, null, 1.0D, 5.0D, () -> 0.5D));
        assertEquals("iron", DuoWeightedRandom.pick(ORES, null, 1.0D, 5.0D, () -> 0.999D));
    }

    @Test
    void mainEntryUsesItsOwnWeight() {
        // gold weighs 8, the others 1 each: copper [0,1), gold [1,9), iron [9,10)
        assertEquals(10.0D, DuoWeightedRandom.totalWeight(ORES, "gold", 8.0D, 1.0D));
        assertEquals("copper", DuoWeightedRandom.pick(ORES, "gold", 8.0D, 1.0D, () -> 0.05D));
        assertEquals("gold", DuoWeightedRandom.pick(ORES, "gold", 8.0D, 1.0D, () -> 0.5D));
        assertEquals("iron", DuoWeightedRandom.pick(ORES, "gold", 8.0D, 1.0D, () -> 0.95D));
    }

    @Test
    void zeroWeightEntriesAreNeverPicked() {
        assertEquals("gold", DuoWeightedRandom.pick(ORES, "gold", 1.0D, 0.0D, () -> 0.0D));
        assertEquals("gold", DuoWeightedRandom.pick(ORES, "gold", 1.0D, 0.0D, () -> 0.999D));
    }

    @Test
    void emptyOrWeightlessTablesYieldNothing() {
        assertNull(DuoWeightedRandom.pick(List.<String>of(), null, 1.0D, 1.0D, () -> 0.5D));
        assertNull(DuoWeightedRandom.pick(ORES, null, 0.0D, 0.0D, () -> 0.5D));
    }

    @Test
    void distributionFollowsWeights() {
        int[] counts = new int[3];
        int samples = 10_000;
        for (int i = 0; i < samples; i++) {
            double roll = (i + 0.5D) / samples;
            counts[ORES.indexOf(DuoWeightedRandom.pick(ORES, "iron", 2.0D, 1.0D, () -> roll))]++;
        }
        assertEquals(2500, counts[0], 1);
        assertEquals(2500, counts[1], 1);
        assertEquals(5000, counts[2], 1);
    }
}
