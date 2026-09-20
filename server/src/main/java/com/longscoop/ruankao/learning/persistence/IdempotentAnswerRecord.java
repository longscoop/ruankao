package com.longscoop.ruankao.learning.persistence;

public record IdempotentAnswerRecord(
        AnswerRecordEntity answer,
        boolean created) {
}
