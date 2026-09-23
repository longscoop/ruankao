package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.PriorityInput;

public final class PriorityScoreCalculator {

    public double calculate(PriorityInput input) {
        validate(input);

        double weakness = 100.0 - input.effectiveMastery();
        double importanceScore = input.importance() * 20.0;
        double forgetting = 100.0 * (1.0 - input.retentionFactor());
        double recentWrongScore = Math.min(100.0, input.recent14dWrongCount() * 20.0);

        double priority = weakness * 0.35
                + importanceScore * 0.25
                + forgetting * 0.20
                + input.examFrequency() * 0.15
                + recentWrongScore * 0.05;

        return Math.round(priority * 100.0) / 100.0;
    }

    private void validate(PriorityInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        if (!inRange(input.effectiveMastery(), 0.0, 100.0)) {
            throw new IllegalArgumentException("effectiveMastery must be between 0 and 100");
        }
        if (input.importance() < 1 || input.importance() > 5) {
            throw new IllegalArgumentException("importance must be between 1 and 5");
        }
        if (!inRange(input.retentionFactor(), 0.0, 1.0)) {
            throw new IllegalArgumentException("retentionFactor must be between 0 and 1");
        }
        if (!inRange(input.examFrequency(), 0.0, 100.0)) {
            throw new IllegalArgumentException("examFrequency must be between 0 and 100");
        }
        if (input.recent14dWrongCount() < 0) {
            throw new IllegalArgumentException("recent14dWrongCount cannot be negative");
        }
    }

    private boolean inRange(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }
}
