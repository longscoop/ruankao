package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.DailyPlanAllocation;

import java.util.Arrays;

public final class DailyPlanAllocator {

    private static final int[] SUPPORTED_TARGETS = {15, 30, 60, 90};
    private static final double[] NORMAL_RATIOS = {0.20, 0.35, 0.25, 0.20};
    private static final double[] SPRINT_RATIOS = {0.30, 0.35, 0.10, 0.25};
    private static final double REMAINDER_EPSILON = 1e-12;

    public DailyPlanAllocation allocate(
            int targetMinutes,
            int daysUntilExam,
            boolean hasDueWrongQuestions) {
        validate(targetMinutes, daysUntilExam);

        double[] ratios = daysUntilExam <= 14 ? SPRINT_RATIOS : NORMAL_RATIOS;
        int[] minutes = largestRemainder(targetMinutes, ratios);

        if (!hasDueWrongQuestions) {
            minutes[1] += minutes[0];
            minutes[0] = 0;
        }

        return new DailyPlanAllocation(minutes[0], minutes[1], minutes[2], minutes[3]);
    }

    private int[] largestRemainder(int targetMinutes, double[] ratios) {
        int[] result = new int[ratios.length];
        double[] remainders = new double[ratios.length];
        int allocated = 0;

        for (int i = 0; i < ratios.length; i++) {
            double exact = targetMinutes * ratios[i];
            result[i] = (int) Math.floor(exact);
            remainders[i] = exact - result[i];
            allocated += result[i];
        }

        int remaining = targetMinutes - allocated;
        boolean[] awarded = new boolean[ratios.length];

        for (int count = 0; count < remaining; count++) {
            int bestIndex = -1;
            double bestRemainder = -1.0;

            for (int i = 0; i < remainders.length; i++) {
                if (!awarded[i] && remainders[i] > bestRemainder + REMAINDER_EPSILON) {
                    bestRemainder = remainders[i];
                    bestIndex = i;
                }
            }

            result[bestIndex]++;
            awarded[bestIndex] = true;
        }

        return result;
    }

    private void validate(int targetMinutes, int daysUntilExam) {
        if (Arrays.stream(SUPPORTED_TARGETS).noneMatch(value -> value == targetMinutes)) {
            throw new IllegalArgumentException("targetMinutes must be one of 15, 30, 60, 90");
        }
        if (daysUntilExam < 0) {
            throw new IllegalArgumentException("daysUntilExam cannot be negative");
        }
    }
}
