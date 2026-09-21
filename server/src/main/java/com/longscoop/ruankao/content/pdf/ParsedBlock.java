package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.content.model.LessonBlockType;

public record ParsedBlock(
        LessonBlockType blockType,
        String textContent,
        String imageObjectKey,
        int sourcePage) {
}
