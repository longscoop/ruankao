package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.PriorityInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityScoreCalculatorTest {

    private final PriorityScoreCalculator calculator = new PriorityScoreCalculator();

    @Test
    void calculatesPriorityUsingTheSpecifiedFormula() {
        double result = calculator.calculate(
                new PriorityInput(40.0, 4, 0.80, 80.0, 3));

        assertEquals(60.0, result);
    }

    @Test
    void lowerEffectiveMasteryRaisesPriority() {
        double weak = calculator.calculate(
                new PriorityInput(30.0, 3, 0.93, 50.0, 1));
        double strong = calculator.calculate(
                new PriorityInput(80.0, 3, 0.93, 50.0, 1));

        assertTrue(weak > strong);
    }

    @Test
    void higherImportanceRaisesPriority() {
        double low = calculator.calculate(
                new PriorityInput(60.0, 1, 0.93, 50.0, 1));
        double high = calculator.calculate(
                new PriorityInput(60.0, 5, 0.93, 50.0, 1));

        assertTrue(high > low);
    }

    @Test
    void recentWrongScoreCapsAtFiveWrongAnswers() {
        double five = calculator.calculate(
                new PriorityInput(60.0, 3, 0.93, 50.0, 5));
        double ten = calculator.calculate(
                new PriorityInput(60.0, 3, 0.93, 50.0, 10));

        assertEquals(five, ten);
    }

    @Test
    void roundsResultToTwoDecimals() {
        double result = calculator.calculate(
                new PriorityInput(63.33, 4, 0.93, 77.7, 2));

        assertEquals(47.71, result);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(-0.01, 3, 0.9, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(100.01, 3, 0.9, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 0, 0.9, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 6, 0.9, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 3, -0.01, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 3, 1.01, 50.0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 3, 0.9, -0.01, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 3, 0.9, 100.01, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new PriorityInput(50.0, 3, 0.9, 50.0, -1)));
    }
}
