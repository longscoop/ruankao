package com.longscoop.ruankao.question;

public record QuestionKnowledgeLink(
        long knowledgeId,
        double weight,
        boolean primary) {
}
