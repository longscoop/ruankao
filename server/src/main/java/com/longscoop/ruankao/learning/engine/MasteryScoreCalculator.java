package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.MasteryUpdateInput;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;

public final class MasteryScoreCalculator {

    private static final double CORRECT_BASE_DELTA = 5.0;
    private static final double WRONG_BASE_DELTA = -6.0;

    public double calculate(double currentScore, MasteryUpdateInput input) {
        validate(currentScore, input);

        double delta = (input.correct() ? CORRECT_BASE_DELTA : WRONG_BASE_DELTA)
                * difficultyFactor(input.correct(), input.difficulty())
                * confidenceFactor(input.correct(), input.confidence())
                * streakFactor(input.correct(), input.streakLength())
                * input.knowledgeWeight();

        return roundToTwoDecimals(clamp(currentScore + delta));
    }

    private void validate(double currentScore, MasteryUpdateInput input) {
        if (!Double.isFinite(currentScore) || currentScore < 0.0 || currentScore > 100.0) {
            throw new IllegalArgumentException("currentScore must be between 0 and 100");
        }
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        if (input.difficulty() == null) {
            throw new IllegalArgumentException("difficulty is required");
        }
        if (input.streakLength() < 1) {
            throw new IllegalArgumentException("streakLength must be at least 1");
        }
        if (!Double.isFinite(input.knowledgeWeight())
                || input.knowledgeWeight() <= 0.0
                || input.knowledgeWeight() > 1.0) {
            throw new IllegalArgumentException("knowledgeWeight must be greater than 0 and at most 1");
        }
    }

    private double difficultyFactor(boolean correct, QuestionDifficulty difficulty) {
        if (correct) {
            return switch (difficulty) {
                case EASY -> 0.8;
                case MEDIUM -> 1.0;
                case HARD -> 1.2;
            };
        }

        return switch (difficulty) {
            case EASY -> 1.3;
            case MEDIUM -> 1.0;
            case HARD -> 0.7;
        };
    }

    private double confidenceFactor(boolean correct, AnswerConfidence confidence) {
        if (confidence == null) {
            return 1.0;
        }

        if (correct) {
            return switch (confidence) {
                case GUESS -> 0.6;
                case UNCERTAIN -> 0.8;
                case CONFIDENT -> 1.0;
            };
        }

        return switch (confidence) {
            case GUESS -> 0.8;
            case UNCERTAIN -> 1.0;
            case CONFIDENT -> 1.2;
        };
    }

    private double streakFactor(boolean correct, int streakLength) {
        if (correct) {
            if (streakLength == 1) {
                return 1.0;
            }
            if (streakLength == 2) {
                return 1.1;
            }
            return 1.2;
        }

        if (streakLength == 1) {
            return 1.0;
        }
        if (streakLength == 2) {
            return 1.2;
        }
        return 1.4;
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
