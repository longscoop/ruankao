package com.longscoop.ruankao.ai.knowledge;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

/** Validates citation identity, not the semantic truth of an LLM's claims. */
public final class GroundedAnswer {
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)\\]");
    private GroundedAnswer() {}
    public record Result(String content, AnswerStatus status, List<Citation> citations) {}

    public static Result compose(String content, List<Citation> sources) {
        if (sources.isEmpty()) {
            return new Result("当前已发布资料中没有找到足够依据。请补充关键词，或联系管理员上传并发布相关资料。",
                    AnswerStatus.NO_EVIDENCE, List.of());
        }
        if (content == null || content.isBlank() || content.length() > 16000) return originals(sources);
        Set<Integer> allowed = new LinkedHashSet<>();
        sources.forEach(source -> allowed.add(source.number()));
        Set<Integer> used = new LinkedHashSet<>();
        var matcher = CITATION.matcher(content);
        while (matcher.find()) {
            try {
                int number = Integer.parseInt(matcher.group(1));
                if (!allowed.contains(number)) return originals(sources);
                used.add(number);
            } catch (NumberFormatException e) { return originals(sources); }
        }
        if (used.isEmpty()) return originals(sources);
        return new Result(content.trim(), AnswerStatus.GROUNDED,
                sources.stream().filter(source -> used.contains(source.number())).toList());
    }

    private static Result originals(List<Citation> sources) {
        StringBuilder text = new StringBuilder("模型回复未通过引用校验，以下仅展示检索到的原文，不作为完整答案：\n");
        for (Citation source : sources) {
            text.append("\n[").append(source.number()).append("] ").append(source.filename())
                    .append(" · ").append(source.filename().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                            ? "第 " + source.page() + " 页" : "文本片段")
                    .append("\n").append(source.text()).append("\n");
        }
        return new Result(text.toString(), AnswerStatus.EXTRACT_ONLY, List.copyOf(sources));
    }
}
