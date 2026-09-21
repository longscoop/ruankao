package com.longscoop.ruankao.question.model;

import com.longscoop.ruankao.learning.model.AnswerSource;

public enum PracticeSource {
    CHAPTER,
    REAL_EXAM,
    WRONG_REVIEW,
    AI_QUIZ;

    public AnswerSource toAnswerSource() {
        return AnswerSource.valueOf(name());
    }
}
