package com.longscoop.ruankao.learning.persistence;

public record MasteryDelta(
        long knowledgeId,
        double scoreDelta,
        boolean correct) {
}
