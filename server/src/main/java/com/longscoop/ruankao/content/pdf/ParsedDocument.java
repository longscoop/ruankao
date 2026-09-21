package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.content.model.ImportDocumentType;

import java.util.List;

public record ParsedDocument(
        String title,
        ImportDocumentType documentType,
        List<ParsedLesson> lessons,
        List<ParsedKnowledgeCandidate> knowledgeCandidates,
        List<ParsedQuestion> questions,
        List<ParsedIssue> issues) {
}
