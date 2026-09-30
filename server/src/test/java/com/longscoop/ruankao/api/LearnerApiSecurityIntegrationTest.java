package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.assessment.AssessmentService;
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
import com.longscoop.ruankao.user.UserExamProfileService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LearnerApiSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserExamProfileService profileService;

    @Autowired
    private AssessmentService assessmentService;

    @Test
    void unauthenticatedLearnerEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/learning/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileUpdateUsesAuthenticatedPrincipalAndIgnoresInjectedUserId() throws Exception {
        long examId = examService.create("api-profile-exam", "API Profile", ExamStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/users/me/exam-profile")
                        .with(user(901L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 9999L,
                                "examId", examId,
                                "examDate", "2026-12-01",
                                "dailyTargetMinutes", 30,
                                "foundationLevel", "SOME"))))
                .andExpect(status().isNoContent());

        assertEquals(examId, profileService.find(901L).orElseThrow().getExamId());
        assertFalse(profileService.find(9999L).isPresent());
    }

    @Test
    void learningTodayReturnsDtoForAuthenticatedUser() throws Exception {
        Fixture fixture = fixture("api-learning", 902L);

        mockMvc.perform(get("/api/v1/learning/today").with(user(fixture.userId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").isNumber())
                .andExpect(jsonPath("$.targetMinutes").value(30))
                .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    void assessmentWithoutPublishedQuestionsReturnsConflict() throws Exception {
        long examId = examService.create(
                "api-empty-assessment", "Empty Assessment", ExamStatus.ACTIVE);
        profileService.upsert(
                905L, examId, LocalDate.now().plusMonths(2), 30,
                com.longscoop.ruankao.user.model.FoundationLevel.SOME);

        mockMvc.perform(post("/api/v1/assessments")
                        .with(user(905L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "examId", examId,
                                "questionCount", 20))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PUBLISHED_QUESTIONS"))
                .andExpect(jsonPath("$.requiredQuestions").value(20))
                .andExpect(jsonPath("$.availableQuestions").value(0));
    }

    @Test
    void assessmentEndpointsUsePrincipalOwnershipAndNeverExposeStandardAnswer() throws Exception {
        Fixture fixture = fixture("api-assessment", 903L);

        String body = mockMvc.perform(post("/api/v1/assessments")
                        .with(user(fixture.userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "examId", fixture.examId(),
                                "questionCount", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID sessionId = UUID.fromString(
                objectMapper.readTree(body).get("sessionId").asText());

        assertEquals(
                fixture.userId(),
                assessmentService.findSession(sessionId).orElseThrow().getUserId());

        mockMvc.perform(get("/api/v1/assessments/{id}/questions", sessionId)
                        .with(user(fixture.userId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.questionId()))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("standardAnswer"))));

        mockMvc.perform(get("/api/v1/assessments/{id}/questions", sessionId)
                        .with(user(904L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/assessments/{id}/answers", sessionId)
                        .with(user(fixture.userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", fixture.questionId(),
                                "answer", "B",
                                "durationSeconds", 12,
                                "confidence", "CONFIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/v1/assessments/{id}/submit", sessionId)
                        .with(user(fixture.userId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.correctQuestions").value(1));
    }

    private Fixture fixture(String prefix, long userId) {
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, prefix + "-kp", "Knowledge", null,
                3, 50, 10, 1, KnowledgeStatus.ACTIVE);
        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Choose B",
                "B",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));

        profileService.upsert(
                userId,
                examId,
                LocalDate.of(2026, 12, 1),
                30,
                com.longscoop.ruankao.user.model.FoundationLevel.SOME);

        return new Fixture(userId, examId, questionId);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private record Fixture(long userId, long examId, long questionId) {
    }
}
