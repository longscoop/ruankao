package com.longscoop.ruankao.content.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfDocumentExtractor {

    public List<ExtractedPdfPage> extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes are required");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<ExtractedPdfPage> pages = new ArrayList<>();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(index + 1);
                stripper.setEndPage(index + 1);
                String text = stripper.getText(document).trim();

                var image = renderer.renderImageWithDPI(index, 110, ImageType.RGB);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                if (!ImageIO.write(image, "png", png)) {
                    throw new IllegalStateException("PNG writer is unavailable");
                }
                pages.add(new ExtractedPdfPage(index + 1, text, png.toByteArray()));
            }
            return pages;
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid or unreadable PDF", e);
        }
    }
}
