package com.longscoop.ruankao.content.pdf;

public record ParsedPage(int pageNumber, String text, String imageObjectKey) {
    public ParsedPage {
        if (pageNumber < 1) throw new IllegalArgumentException("pageNumber must be positive");
        text = text == null ? "" : text;
    }
}
