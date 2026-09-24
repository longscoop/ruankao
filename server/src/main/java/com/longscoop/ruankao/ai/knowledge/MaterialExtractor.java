package com.longscoop.ruankao.ai.knowledge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MaterialExtractor {
    public static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final int MAX_PAGES = 500;
    private static final int MAX_CHARACTERS = 2_000_000;

    public record Extraction(List<KnowledgeText.Page> pages, int pageCount, List<String> warnings) {}

    public Extraction extract(String filename, byte[] data) {
        if (filename == null || filename.isBlank() || filename.length() > 240) {
            throw new IllegalArgumentException("文件名不能为空且不能超过 240 字符");
        }
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            throw new IllegalArgumentException("文件不能为空且不能超过 20 MiB");
        }
        String name = filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) return pdf(data);
        if (!name.endsWith(".txt") && !name.endsWith(".md")) {
            throw new IllegalArgumentException("仅支持文本型 PDF、UTF-8 TXT 和 Markdown");
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            validateText(text);
            return new Extraction(List.of(new KnowledgeText.Page(1, text)), 1, List.of());
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("TXT/Markdown 必须使用 UTF-8 编码", e);
        }
    }

    private Extraction pdf(byte[] data) {
        try (var document = Loader.loadPDF(data)) {
            if (document.isEncrypted() || !document.getCurrentAccessPermission().canExtractContent()) {
                throw new IllegalArgumentException("不支持加密或禁止提取内容的 PDF");
            }
            int count = document.getNumberOfPages();
            if (count < 1 || count > MAX_PAGES) {
                throw new IllegalArgumentException("PDF 页数必须在 1 到 500 之间，请拆分后上传");
            }
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<KnowledgeText.Page> pages = new ArrayList<>();
            List<Integer> emptyPages = new ArrayList<>();
            int characters = 0;
            for (int page = 1; page <= count; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).replace("\u0000", "");
                characters += text.length();
                if (characters > MAX_CHARACTERS) {
                    throw new IllegalArgumentException("提取文字超过 200 万字符，请拆分后上传");
                }
                if (text.isBlank()) emptyPages.add(page);
                else pages.add(new KnowledgeText.Page(page, text));
            }
            if (pages.isEmpty()) {
                throw new IllegalArgumentException("PDF 没有可提取文字，扫描资料请先进行 OCR");
            }
            List<String> warnings = emptyPages.isEmpty() ? List.of() : List.of(
                    "以下页没有提取到文字，可能为空白或扫描页，未参与问答；请检查并按需 OCR："
                            + emptyPages.stream().limit(50).toList()
                            + (emptyPages.size() > 50 ? "（共 " + emptyPages.size() + " 页）" : ""));
            return new Extraction(List.copyOf(pages), count, warnings);
        } catch (IOException e) {
            throw new IllegalArgumentException("PDF 无法读取，请检查文件是否损坏、加密或格式错误", e);
        }
    }

    private void validateText(String text) {
        if (text.isBlank()) throw new IllegalArgumentException("资料没有可提取文字");
        if (text.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("提取文字超过 200 万字符，请拆分后上传");
        }
        if (text.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("资料包含二进制内容，请上传 UTF-8 文本");
        }
    }
}
