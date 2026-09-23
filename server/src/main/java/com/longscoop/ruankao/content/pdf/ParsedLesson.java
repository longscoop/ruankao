package com.longscoop.ruankao.content.pdf;

import java.util.List;

public record ParsedLesson(
        String key,
        String title,
        int sourcePageStart,
        int sourcePageEnd,
        List<ParsedBlock> blocks) {
}
