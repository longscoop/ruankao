package com.longscoop.ruankao.question;

import java.util.UUID;

public record QuestionAnswerResult(
        UUID answerId,
        boolean correct,
        String correctAnswer) {
}
