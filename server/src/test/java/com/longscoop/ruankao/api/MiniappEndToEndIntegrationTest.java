package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.ai.AiProvider;
import com.longscoop.ruankao.ai.AiProviderResponse;
import com.longscoop.ruankao.auth.WechatIdentity;
import com.longscoop.ruankao.auth.WechatIdentityProvider;
import com.longscoop.ruankao.course.CourseService;
import com.longscoop.ruankao.course.VideoService;
import com.longscoop.ruankao.course.VideoTranscriptService;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
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
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MiniappEndToEndIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired QuestionService questionService;
    @Autowired QuestionMapper questionMapper;
    @Autowired CourseService courseService;
    @Autowired VideoService videoService;
    @Autowired VideoTranscriptService transcriptService;

    @MockBean WechatIdentityProvider wechatIdentityProvider;
    @MockBean StorageProvider storageProvider;
    @MockBean AiProvider aiProvider;

    @Test
    void loginThroughWeeklyReportWorksWithRealPersistence() throws Exception {
        long examId = examService.create("e2e-system-architect", "系统架构设计师", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "e2e-architecture", "软件架构", "架构核心知识",
                5, 85, 12, 1, KnowledgeStatus.ACTIVE);
        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "以下哪项描述正确？",
                "B",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));
        QuestionEntity question = questionMapper.selectById(questionId);
        question.setOptionsJson("{\"A\":\"错误项\",\"B\":\"正确项\",\"C\":\"干扰项\"}");
        question.setExplanation("B 符合题目考查的架构原则。");
        questionMapper.updateById(question);

        long courseId = courseService.createCourse(
                examId, "系统架构基础", "核心课程", CourseStatus.PUBLISHED, 1);
        long chapterId = courseService.createChapter(courseId, "架构概述", null, 1);
        long videoId = videoService.createVideo(
                chapterId, "架构基础视频", null, "videos/e2e.mp4",
                100, true, VideoStatus.WAITING_REVIEW, 1);
        videoService.linkKnowledge(videoId, knowledgeId);
        videoService.updateStatus(videoId, VideoStatus.PUBLISHED);
        transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(
                        BigDecimal.ZERO, BigDecimal.TEN, "架构基础")));

        when(wechatIdentityProvider.exchangeCode("e2e-code"))
                .thenReturn(new WechatIdentity("e2e-openid", "e2e-unionid"));
        when(storageProvider.generateAccessUrl(anyString(), any(Duration.class)))
                .thenReturn(URI.create("https://cdn.example.test/videos/e2e.mp4"));
        when(aiProvider.complete(any()))
                .thenReturn(new AiProviderResponse(
                        "qwen", "qwen-test", "这是 AI 助教解释。", 20, 10));

        JsonNode login = json(mockMvc.perform(post("/api/v1/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"e2e-code\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();

        mockMvc.perform(get("/api/v1/exams").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(examId));

        mockMvc.perform(put("/api/v1/users/me/exam-profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "examId", examId,
                                "examDate", LocalDate.now().plusMonths(2).toString(),
                                "dailyTargetMinutes", 30,
                                "foundationLevel", "SOME"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/exam-profile")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(examId))
                .andExpect(jsonPath("$.foundationLevel").value("SOME"));

        JsonNode assessment = json(mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":" + examId + ",\"questionCount\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String assessmentId = assessment.get("sessionId").asText();

        mockMvc.perform(get("/api/v1/assessments/{id}/questions", assessmentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options[1].key").value("B"));

        mockMvc.perform(post("/api/v1/assessments/{id}/answers", assessmentId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "B",
                                "durationSeconds", 12,
                                "confidence", "CONFIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/v1/assessments/{id}/submit", assessmentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/learning/today")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isArray());

        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(courseId));

        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].videos[0].id").value(videoId));

        mockMvc.perform(get("/api/v1/videos/{id}", videoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playUrl").value("https://cdn.example.test/videos/e2e.mp4"));

        mockMvc.perform(put("/api/v1/videos/{id}/progress", videoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressSeconds\":85}"))
                .andExpect(status().isNoContent());

        JsonNode practice = json(mockMvc.perform(post("/api/v1/question-sessions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"REAL_EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String practiceId = practice.get("sessionId").asText();

        mockMvc.perform(get("/api/v1/question-sessions/{id}/questions", practiceId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options[1].text").value("正确项"));

        mockMvc.perform(post("/api/v1/question-sessions/{id}/answers", practiceId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "A",
                                "durationSeconds", 8,
                                "confidence", "CONFIDENT",
                                "source", "REAL_EXAM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.standardAnswer").value("B"));

        mockMvc.perform(get("/api/v1/users/me/wrong-questions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(questionId));

        mockMvc.perform(put("/api/v1/users/me/favorites/{id}", questionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question.id").value(questionId));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"解释架构风格\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("这是 AI 助教解释。"));

        mockMvc.perform(get("/api/v1/users/me/stats/weekly")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredQuestions", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.weakKnowledge").isArray());
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
