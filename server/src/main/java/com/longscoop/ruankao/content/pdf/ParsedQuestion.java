package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.question.model.QuestionType;

import java.util.Map;

public record ParsedQuestion(
        String key,
        String questionNo,
        QuestionType questionType,
        String content,
        Map<String, String> options,
        String answer,
        String explanation,
        String sourceLabel,
        int sourcePageStart,
        int sourcePageEnd,
        boolean requiresReview) {
}
