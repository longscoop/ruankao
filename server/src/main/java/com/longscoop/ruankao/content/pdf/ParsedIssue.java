package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.content.model.ImportIssueSeverity;

public record ParsedIssue(
        ImportIssueSeverity severity,
        String code,
        String message,
        Integer sourcePage,
        String itemKey) {
}
