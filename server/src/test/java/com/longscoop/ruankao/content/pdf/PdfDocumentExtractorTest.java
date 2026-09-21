package com.longscoop.ruankao.content.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentExtractorTest {

    @Test
    void extractsPagesAndRendersPngSnapshots() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText("Lambda architecture lecture");
                stream.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            pdf = output.toByteArray();
        }

        PdfDocumentExtractor extractor = new PdfDocumentExtractor();
        List<ExtractedPdfPage> pages = extractor.extract(pdf);

        assertEquals(1, pages.size());
        assertTrue(pages.get(0).text().contains("Lambda architecture lecture"));
        assertTrue(pages.get(0).pngBytes().length > 100);
        assertEquals(1, pages.get(0).pageNumber());
    }
}
