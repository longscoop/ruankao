package com.longscoop.ruankao.ai;

import com.longscoop.ruankao.ai.persistence.AiUsageLogEntity;
import com.longscoop.ruankao.ai.persistence.AiUsageLogMapper;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AiService {

    private static final String STUDY_SYSTEM_PROMPT =
            "你是软考系统架构设计师学习助教。回答要准确、简洁、面向备考。"
            + "你只能解释和辅导，不得声称修改用户掌握度、标准答案或会员状态。";

    private final AiProvider aiProvider;
    private final AiUsageLogMapper usageLogMapper;
    private final QuestionService questionService;
    private final int dailyRequestLimit;

    public AiService(
            AiProvider aiProvider,
            AiUsageLogMapper usageLogMapper,
            QuestionService questionService,
            @Value("${AI_DAILY_REQUEST_LIMIT:20}") int dailyRequestLimit) {
        this.aiProvider = aiProvider;
        this.usageLogMapper = usageLogMapper;
        this.questionService = questionService;
        this.dailyRequestLimit = Math.max(1, dailyRequestLimit);
    }

    public AiResult chat(long userId, String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }
        return execute(
                userId,
                "CHAT",
                new AiProviderRequest(STUDY_SYSTEM_PROMPT, message.trim()));
    }

    public AiResult suggestContentImportStructure(long userId, String sourceJson) {
        if (sourceJson == null || sourceJson.isBlank()) {
            throw new IllegalArgumentException("sourceJson is required");
        }
        String systemPrompt = """
                你是软考内容导入审核助手。你只能分析结构和指出疑点。
                禁止改写、补全或纠正原始题干、原始选项、原始答案和原始解析。
                禁止把模拟题推断成真题，禁止自动决定知识点重要度、考试频率或题目难度。
                只返回 JSON 建议，字段包括 warnings、suggestedStructure、confidence。
                """;
        String prompt = """
                请检查下面由 PDF 规则解析器得到的结构化数据。
                只指出结构问题、字段缺失、组合题拆分建议或可能需要人工核对的位置。
                原始结构化数据：
                %s
                """.formatted(sourceJson.trim());
        return execute(
                userId,
                "CONTENT_IMPORT_STRUCTURE",
                new AiProviderRequest(systemPrompt, prompt));
    }

    public AiResult explainQuestion(long userId, long questionId) {
        QuestionEntity question = questionService.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("question not found"));
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException("question is not published");
        }

        String prompt = """
                请解释下面这道软考题。标准答案由系统提供，你不能修改或质疑数据库中的标准答案。
                需要说明：为什么正确选项正确、常见误区、相关知识点如何记忆。
                
                题目：
                %s
                
                标准答案：
                %s
                
                已审核解析（如有）：
                %s
                """.formatted(
                question.getContent(),
                question.getStandardAnswer(),
                question.getExplanation() == null ? "无" : question.getExplanation());

        return execute(
                userId,
                "QUESTION_EXPLAIN",
                new AiProviderRequest(STUDY_SYSTEM_PROMPT, prompt));
    }

    private AiResult execute(long userId, String function, AiProviderRequest request) {
        OffsetDateTime todayStart = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
        if (usageLogMapper.countSuccessfulSince(userId, todayStart) >= dailyRequestLimit) {
            throw new AiQuotaExceededException("daily AI request quota exceeded");
        }

        UUID requestId = UUID.randomUUID();
        long startedAt = System.nanoTime();
        try {
            AiProviderResponse response = aiProvider.complete(request);
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            persistUsage(
                    userId, requestId, function,
                    response.provider(), response.model(),
                    response.promptTokens(), response.completionTokens(),
                    latencyMs, true, null);
            return new AiResult(requestId, response.content());
        } catch (RuntimeException e) {
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            persistUsage(
                    userId, requestId, function,
                    safe(aiProvider.providerName()), safe(aiProvider.modelName()),
                    0, 0, latencyMs, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    @Transactional
    protected void persistUsage(
            long userId,
            UUID requestId,
            String function,
            String provider,
            String model,
            int promptTokens,
            int completionTokens,
            long latencyMs,
            boolean success,
            String errorCode) {
        AiUsageLogEntity entity = new AiUsageLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setRequestId(requestId);
        entity.setProvider(safe(provider));
        entity.setModel(safe(model));
        entity.setFunction(function);
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setLatencyMs(Math.max(0, latencyMs));
        entity.setEstimatedCost(BigDecimal.ZERO);
        entity.setSuccess(success);
        entity.setErrorCode(errorCode);
        usageLogMapper.insert(entity);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    public record AiResult(UUID requestId, String content) {
    }
}
