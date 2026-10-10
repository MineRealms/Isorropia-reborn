package fr.wind_blade.isorropia.common.config;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * Weighted pick where one optional "main" entry uses its own weight and every other entry uses a shared
 * auxiliary weight. Without a main entry all entries weigh {@code weight}.
 */
public final class DuoWeightedRandom {
    private DuoWeightedRandom() {
    }

    public static double totalWeight(List<?> entries, Object main, double mainWeight, double otherWeight) {
        double total = 0.0D;
        for (Object entry : entries) {
            total += weightOf(entry, main, mainWeight, otherWeight);
        }
        return total;
    }

    /** Returns null when the list is empty or every weight is zero. */
    public static <T> T pick(List<T> entries, T main, double mainWeight, double otherWeight, DoubleSupplier unitRandom) {
        Objects.requireNonNull(entries, "entries");
        double total = totalWeight(entries, main, mainWeight, otherWeight);
        if (total <= 0.0D) {
            return null;
        }
        double roll = unitRandom.getAsDouble() * total;
        T last = null;
        for (T entry : entries) {
            double weight = weightOf(entry, main, mainWeight, otherWeight);
            if (weight <= 0.0D) {
                continue;
            }
            last = entry;
            roll -= weight;
            if (roll < 0.0D) {
                return entry;
            }
        }
        return last;
    }

    private static double weightOf(Object entry, Object main, double mainWeight, double otherWeight) {
        if (main == null) {
            return mainWeight;
        }
        return entry.equals(main) ? mainWeight : otherWeight;
    }
}
