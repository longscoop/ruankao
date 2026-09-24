package com.longscoop.ruankao.ai.knowledge;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterialExtractorTest {
    private final MaterialExtractor extractor = new MaterialExtractor();

    @Test
    void readsUtf8TextAndMarkdownWithoutInventingPages() {
        var result = extractor.extract("架构.MD", "# 架构设计\n分层架构".getBytes(StandardCharsets.UTF_8));
        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).number()).isEqualTo(1);
        assertThat(result.pages().get(0).text()).contains("分层架构");
    }

    @Test
    void rejectsInvalidEncodingUnsupportedFilesAndOversizeUploads() {
        assertThatThrownBy(() -> extractor.extract("notes.txt", new byte[]{(byte) 0xff}))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UTF-8");
        assertThatThrownBy(() -> extractor.extract("notes.exe", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> extractor.extract("notes.txt", new byte[20 * 1024 * 1024 + 1]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> extractor.extract("notes.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsPdfPageNumbersAndWarnsAboutPagesWithoutText() throws Exception {
        var result = extractor.extract("architecture.pdf", pdf(true));
        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).number()).isEqualTo(2);
        assertThat(result.pages().get(0).text()).contains("Architecture");
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void rejectsPdfWithoutExtractableTextInsteadOfPretendingSuccess() throws Exception {
        assertThatThrownBy(() -> extractor.extract("scan.pdf", pdf(false)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("OCR");
    }

    private byte[] pdf(boolean withText) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            if (withText) {
                var page = new PDPage();
                document.addPage(page);
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(30, 700);
                    stream.showText("Architecture and design patterns");
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
