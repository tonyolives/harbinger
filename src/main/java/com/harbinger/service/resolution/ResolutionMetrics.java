package com.harbinger.service.resolution;

/**
 * Pairwise accuracy of a resolution run: how well the predicted clusters recover the
 * true groupings. Computed by {@link ResolutionService#evaluate} from pair counts —
 * a pair of signals is "together" if they share a cluster (predicted) or a
 * {@code trueOwnerId} (truth).
 *
 * <ul>
 *   <li><b>precision</b> = correctly-grouped pairs / predicted pairs (penalizes over-merging)</li>
 *   <li><b>recall</b> = correctly-grouped pairs / true pairs (penalizes under-merging)</li>
 *   <li><b>f1</b> = harmonic mean of the two</li>
 * </ul>
 */
public record ResolutionMetrics(
        double precision,
        double recall,
        double f1,
        long truePositivePairs,
        long predictedPairs,
        long truePairs
) {
    public ResolutionMetrics {
        requireRatio(precision, "precision");
        requireRatio(recall, "recall");
        requireRatio(f1, "f1");
        requireNonNegative(truePositivePairs, "truePositivePairs");
        requireNonNegative(predictedPairs, "predictedPairs");
        requireNonNegative(truePairs, "truePairs");
    }

    /**
     * Builds metrics from raw pair counts. When a denominator is 0 (e.g. every cluster is
     * a singleton, so there are no predicted pairs) the corresponding ratio is defined as
     * 1.0 — there is nothing to get wrong.
     */
    public static ResolutionMetrics of(long truePositivePairs, long predictedPairs, long truePairs) {
        double precision = predictedPairs == 0 ? 1.0 : (double) truePositivePairs / predictedPairs;
        double recall = truePairs == 0 ? 1.0 : (double) truePositivePairs / truePairs;
        double f1 = (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        return new ResolutionMetrics(precision, recall, f1, truePositivePairs, predictedPairs, truePairs);
    }

    private static void requireRatio(double value, String field) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
