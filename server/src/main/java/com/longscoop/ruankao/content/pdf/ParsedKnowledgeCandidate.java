package com.longscoop.ruankao.content.pdf;

public record ParsedKnowledgeCandidate(
        String key,
        String name,
        int sourcePage,
        int sortOrder) {
}
