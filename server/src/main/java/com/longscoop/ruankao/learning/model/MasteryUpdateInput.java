package com.longscoop.ruankao.learning.model;

public record MasteryUpdateInput(
        boolean correct,
        QuestionDifficulty difficulty,
        AnswerConfidence confidence,
        int streakLength,
        double knowledgeWeight) {
}
