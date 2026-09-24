package com.longscoop.ruankao.ai.knowledge;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic lexical indexing, not semantic embeddings. */
public final class KnowledgeText {
    private static final int WINDOW = 800;
    private static final int OVERLAP = 120;
    private static final Pattern TOKEN = Pattern.compile("\\p{IsHan}+|[a-z0-9]+");

    private KnowledgeText() {}

    public record Page(int number, String text) {}
    public record Chunk(int page, int ordinal, String text, String searchText) {}

    public static List<String> terms(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        Set<String> result = new LinkedHashSet<>();
        var matcher = TOKEN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            int[] points = token.codePoints().toArray();
            if (Character.UnicodeScript.of(points[0]) == Character.UnicodeScript.HAN) {
                if (points.length == 1) result.add(token);
                for (int i = 0; i + 1 < points.length; i++) {
                    result.add(new String(points, i, 2));
                }
            } else if (token.length() <= 64) {
                result.add(token);
            }
        }
        return List.copyOf(result);
    }

    /** Only tokenizer-produced letters and digits reach tsquery, never user operators. */
    public static String tsQuery(String text) {
        return String.join(" | ", terms(text).stream().limit(64).toList());
    }

    public static List<Chunk> split(List<Page> pages) {
        if (pages == null) throw new IllegalArgumentException("资料页不能为空");
        List<Chunk> result = new ArrayList<>();
        for (Page page : pages) {
            if (page == null || page.number() < 1 || page.text() == null) {
                throw new IllegalArgumentException("资料页格式无效");
            }
            String text = page.text().strip();
            if (text.isBlank()) continue;
            for (int start = 0; start < text.length();) {
                int end = Math.min(start + WINDOW, text.length());
                if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
                String part = text.substring(start, end);
                String searchText = String.join(" ", terms(part));
                if (!searchText.isBlank()) {
                    result.add(new Chunk(page.number(), result.size(), part, searchText));
                }
                if (end == text.length()) break;
                start = end - OVERLAP;
                if (Character.isLowSurrogate(text.charAt(start))) start++;
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("资料没有可索引的文字，请检查内容或先进行 OCR");
        return List.copyOf(result);
    }
}
