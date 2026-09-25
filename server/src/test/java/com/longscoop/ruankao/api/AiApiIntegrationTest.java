package com.longscoop.ruankao.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.ai.AiProvider;
import com.longscoop.ruankao.ai.AiProviderResponse;
import com.longscoop.ruankao.ai.persistence.AiUsageLogEntity;
import com.longscoop.ruankao.ai.persistence.AiUsageLogMapper;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired QuestionService questionService;
    @Autowired AiUsageLogMapper usageLogMapper;
    @Autowired com.longscoop.ruankao.ai.AiService aiService;

    @MockBean AiProvider aiProvider;

    @Test
    void chatAndQuestionExplainUseProviderAndPersistUsage() throws Exception {
        when(aiProvider.complete(any())).thenReturn(
                new AiProviderResponse("Qwen", "qwen-test", "清晰的解释", 12, 8));

        long examId = examService.create("ai-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "ai-kp", "Architecture", null,
                4, 70, 10, 1, KnowledgeStatus.ACTIVE);
        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Which is correct?",
                "B",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(1401L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "解释一下架构风格"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("清晰的解释"))
                .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(post("/api/v1/ai/question-explain")
                        .with(user(1401L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("清晰的解释"));

        assertEquals(2, usageLogMapper.selectCount(
                new QueryWrapper<AiUsageLogEntity>().eq("user_id", 1401L)));
    }


    @Test
    void contentImportSuggestionUsesAiWithoutMutatingSource() {
        when(aiProvider.complete(any())).thenReturn(
                new AiProviderResponse("Qwen", "qwen-test",
                        "{\"warnings\":[\"check composite structure\"]}", 10, 6));

        String source = "{\"itemType\":\"QUESTION\",\"answer\":\"C\"}";
        var result = aiService.suggestContentImportStructure(1402L, source);

        assertEquals("{\"warnings\":[\"check composite structure\"]}", result.content());
        assertEquals(source, "{\"itemType\":\"QUESTION\",\"answer\":\"C\"}");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
