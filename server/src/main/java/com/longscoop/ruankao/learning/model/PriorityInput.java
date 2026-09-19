package com.longscoop.ruankao.learning.model;

public record PriorityInput(
        double effectiveMastery,
        int importance,
        double retentionFactor,
        double examFrequency,
        int recent14dWrongCount) {
}
