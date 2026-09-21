package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.QuestionAnswerService;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeeklyStatsApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired QuestionService questionService;
    @Autowired QuestionAnswerService answerService;
    @Autowired UserExamProfileService profileService;

    @Test
    void weeklyStatsAggregatePersistedAnswersAndWeakKnowledge() throws Exception {
        long userId = 1501L;
        long examId = examService.create("weekly-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "weekly-kp", "Architecture", null,
                5, 80, 10, 1, KnowledgeStatus.ACTIVE);
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
                userId, examId, LocalDate.now().plusMonths(2), 30, FoundationLevel.SOME);

        answerService.submit(
                userId,
                questionId,
                UUID.randomUUID(),
                "weekly-answer-1",
                "B",
                120,
                AnswerConfidence.CONFIDENT,
                AnswerSource.REAL_EXAM);

        mockMvc.perform(get("/api/v1/users/me/stats/weekly").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyMinutes").value(2))
                .andExpect(jsonPath("$.answeredQuestions").value(1))
                .andExpect(jsonPath("$.correctQuestions").value(1))
                .andExpect(jsonPath("$.weakKnowledge[0].knowledgeId").value(knowledgeId))
                .andExpect(jsonPath("$.weakKnowledge[0].evidenceCount").value(1))
                .andExpect(jsonPath("$.weakKnowledge[0].masteryScore").value(5.0));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
