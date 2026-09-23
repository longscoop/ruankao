package com.longscoop.ruankao.learning.engine;

public final class RetentionCalculator {

    public double retentionFactor(long daysSinceLastEffectiveStudy) {
        if (daysSinceLastEffectiveStudy < 0) {
            throw new IllegalArgumentException("daysSinceLastEffectiveStudy cannot be negative");
        }
        if (daysSinceLastEffectiveStudy <= 3) {
            return 1.00;
        }
        if (daysSinceLastEffectiveStudy <= 7) {
            return 0.97;
        }
        if (daysSinceLastEffectiveStudy <= 14) {
            return 0.93;
        }
        if (daysSinceLastEffectiveStudy <= 30) {
            return 0.88;
        }
        if (daysSinceLastEffectiveStudy <= 60) {
            return 0.80;
        }
        return 0.72;
    }

    public double effectiveMastery(double masteryScore, long daysSinceLastEffectiveStudy) {
        if (!Double.isFinite(masteryScore) || masteryScore < 0.0 || masteryScore > 100.0) {
            throw new IllegalArgumentException("masteryScore must be between 0 and 100");
        }

        double effective = masteryScore * retentionFactor(daysSinceLastEffectiveStudy);
        return Math.round(effective * 100.0) / 100.0;
    }
}
