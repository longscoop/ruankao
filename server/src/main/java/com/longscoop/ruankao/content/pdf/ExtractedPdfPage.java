package com.longscoop.ruankao.content.pdf;

public record ExtractedPdfPage(int pageNumber, String text, byte[] pngBytes) {
}
