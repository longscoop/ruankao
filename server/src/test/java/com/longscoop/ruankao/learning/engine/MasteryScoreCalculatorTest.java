package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.MasteryUpdateInput;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasteryScoreCalculatorTest {

    private final MasteryScoreCalculator calculator = new MasteryScoreCalculator();

    @Test
    void mediumConfidentCorrectAddsFivePoints() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, AnswerConfidence.CONFIDENT, 1, 1.0));

        assertEquals(55.0, result);
    }

    @Test
    void secondMediumConfidentWrongAnswerSubtractsEightPointSixFour() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(false, QuestionDifficulty.MEDIUM, AnswerConfidence.CONFIDENT, 2, 1.0));

        assertEquals(41.36, result);
    }

    @Test
    void easyWrongAnswerIsPenalizedMoreThanHardWrongAnswer() {
        double easy = calculator.calculate(
                50.0,
                new MasteryUpdateInput(false, QuestionDifficulty.EASY, AnswerConfidence.UNCERTAIN, 1, 1.0));
        double hard = calculator.calculate(
                50.0,
                new MasteryUpdateInput(false, QuestionDifficulty.HARD, AnswerConfidence.UNCERTAIN, 1, 1.0));

        assertTrue(easy < hard);
    }

    @Test
    void guessedCorrectAnswerGainsLessThanConfidentCorrectAnswer() {
        double guessed = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, AnswerConfidence.GUESS, 1, 1.0));
        double confident = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, AnswerConfidence.CONFIDENT, 1, 1.0));

        assertTrue(guessed < confident);
    }

    @Test
    void thirdCorrectAnswerUsesOnePointTwoStreakFactor() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, AnswerConfidence.CONFIDENT, 3, 1.0));

        assertEquals(56.0, result);
    }

    @Test
    void thirdWrongAnswerUsesOnePointFourStreakFactor() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(false, QuestionDifficulty.MEDIUM, null, 3, 1.0));

        assertEquals(41.6, result);
    }

    @Test
    void knowledgeWeightScalesDelta() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, AnswerConfidence.CONFIDENT, 1, 0.5));

        assertEquals(52.5, result);
    }

    @Test
    void missingConfidenceUsesNeutralFactor() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 1, 1.0));

        assertEquals(55.0, result);
    }

    @Test
    void clampsScoreToUpperBound() {
        double result = calculator.calculate(
                98.0,
                new MasteryUpdateInput(true, QuestionDifficulty.HARD, AnswerConfidence.CONFIDENT, 3, 1.0));

        assertEquals(100.0, result);
    }

    @Test
    void clampsScoreToLowerBound() {
        double result = calculator.calculate(
                2.0,
                new MasteryUpdateInput(false, QuestionDifficulty.EASY, AnswerConfidence.CONFIDENT, 3, 1.0));

        assertEquals(0.0, result);
    }

    @Test
    void roundsFinalScoreToTwoDecimals() {
        double result = calculator.calculate(
                50.0,
                new MasteryUpdateInput(true, QuestionDifficulty.HARD, AnswerConfidence.UNCERTAIN, 2, 0.33));

        assertEquals(51.74, result);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(-0.01,
                        new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 1, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(100.01,
                        new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 1, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(50.0,
                        new MasteryUpdateInput(true, null, null, 1, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(50.0,
                        new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 0, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(50.0,
                        new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 1, 0.0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(50.0,
                        new MasteryUpdateInput(true, QuestionDifficulty.MEDIUM, null, 1, 1.01)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(50.0, null));
    }
}
