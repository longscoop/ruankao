package com.longscoop.ruankao.learning.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetentionCalculatorTest {

    private final RetentionCalculator calculator = new RetentionCalculator();

    @Test
    void returnsExactRetentionFactorsAtBoundaries() {
        assertEquals(1.00, calculator.retentionFactor(0));
        assertEquals(1.00, calculator.retentionFactor(3));
        assertEquals(0.97, calculator.retentionFactor(4));
        assertEquals(0.97, calculator.retentionFactor(7));
        assertEquals(0.93, calculator.retentionFactor(8));
        assertEquals(0.93, calculator.retentionFactor(14));
        assertEquals(0.88, calculator.retentionFactor(15));
        assertEquals(0.88, calculator.retentionFactor(30));
        assertEquals(0.80, calculator.retentionFactor(31));
        assertEquals(0.80, calculator.retentionFactor(60));
        assertEquals(0.72, calculator.retentionFactor(61));
        assertEquals(0.72, calculator.retentionFactor(365));
    }

    @Test
    void calculatesEffectiveMasteryWithoutMutatingStoredMastery() {
        assertEquals(79.2, calculator.effectiveMastery(90.0, 20));
        assertEquals(64.8, calculator.effectiveMastery(90.0, 90));
    }

    @Test
    void roundsEffectiveMasteryToTwoDecimals() {
        assertEquals(61.47, calculator.effectiveMastery(66.1, 10));
    }

    @Test
    void acceptsMasteryBounds() {
        assertEquals(0.0, calculator.effectiveMastery(0.0, 10));
        assertEquals(100.0, calculator.effectiveMastery(100.0, 0));
    }

    @Test
    void rejectsNegativeDays() {
        assertThrows(IllegalArgumentException.class, () -> calculator.retentionFactor(-1));
        assertThrows(IllegalArgumentException.class, () -> calculator.effectiveMastery(50.0, -1));
    }

    @Test
    void rejectsInvalidMastery() {
        assertThrows(IllegalArgumentException.class, () -> calculator.effectiveMastery(-0.01, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.effectiveMastery(100.01, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.effectiveMastery(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.effectiveMastery(Double.POSITIVE_INFINITY, 1));
    }
}
