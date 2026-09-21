package com.longscoop.ruankao.api;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FavoriteQuestionApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired QuestionService questionService;
    @Autowired UserExamProfileService profileService;

    @Test
    void favoritePersistsAndCanBeRemoved() throws Exception {
        long userId = 1701L;
        long examId = examService.create("favorite-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "favorite-kp", "Architecture", null,
                4, 70, 10, 1, KnowledgeStatus.ACTIVE);
        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Favorite me",
                "A",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));
        profileService.upsert(
                userId, examId, LocalDate.now().plusMonths(2), 30, FoundationLevel.SOME);

        mockMvc.perform(put("/api/v1/users/me/favorites/{questionId}", questionId)
                        .with(user(userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/favorites").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question.id").value(questionId))
                .andExpect(jsonPath("$[0].savedAt").isString());

        mockMvc.perform(delete("/api/v1/users/me/favorites/{questionId}", questionId)
                        .with(user(userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/favorites").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
