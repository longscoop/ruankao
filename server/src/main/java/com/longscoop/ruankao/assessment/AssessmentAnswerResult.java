package com.longscoop.ruankao.assessment;

import java.util.UUID;

public record AssessmentAnswerResult(
        long questionId,
        boolean correct,
        UUID answerRecordId) {
}
