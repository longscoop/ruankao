package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PracticeApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired QuestionService questionService;
    @Autowired QuestionMapper questionMapper;
    @Autowired UserExamProfileService profileService;

    @Test
    void realExamSessionExposesOptionsThenPersistsWrongQuestion() throws Exception {
        long userId = 1301L;
        long examId = examService.create("practice-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "practice-kp", "Architecture", null,
                5, 80, 10, 1, KnowledgeStatus.ACTIVE);
        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Which option is correct?",
                "B",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));

        QuestionEntity question = questionMapper.selectById(questionId);
        question.setOptionsJson("{\"A\":\"Wrong\",\"B\":\"Correct\",\"C\":\"Wrong too\"}");
        question.setExplanation("B is correct because it matches the architecture rule.");
        questionMapper.updateById(question);

        profileService.upsert(
                userId, examId, LocalDate.now().plusMonths(2), 30, FoundationLevel.SOME);

        String startBody = mockMvc.perform(post("/api/v1/question-sessions")
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("source", "REAL_EXAM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isString())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(startBody).get("sessionId").asText();

        mockMvc.perform(get("/api/v1/question-sessions/{id}/questions", sessionId)
                        .with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(questionId))
                .andExpect(jsonPath("$[0].options[0].key").value("A"))
                .andExpect(jsonPath("$[0].options[1].key").value("B"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("standardAnswer"))));

        mockMvc.perform(post("/api/v1/question-sessions/{id}/answers", sessionId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "A",
                                "durationSeconds", 9,
                                "confidence", "CONFIDENT",
                                "source", "REAL_EXAM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.standardAnswer").value("B"))
                .andExpect(jsonPath("$.explanation").value(
                        "B is correct because it matches the architecture rule."));

        mockMvc.perform(get("/api/v1/users/me/wrong-questions").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(questionId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].question.content").value("Which option is correct?"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
