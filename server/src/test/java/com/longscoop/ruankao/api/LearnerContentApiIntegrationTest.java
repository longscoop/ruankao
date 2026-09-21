package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.course.CourseService;
import com.longscoop.ruankao.course.VideoService;
import com.longscoop.ruankao.course.VideoTranscriptService;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LearnerContentApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired CourseService courseService;
    @Autowired VideoService videoService;
    @Autowired VideoTranscriptService transcriptService;
    @Autowired UserExamProfileService profileService;

    @MockBean StorageProvider storageProvider;

    @Test
    void courseVideoAndKnowledgeFlowUsesPublishedContentAndRealProgress() throws Exception {
        long userId = 1201L;
        long examId = examService.create("content-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "content-kp", "Architecture", "Core architecture concepts",
                5, 80, 12, 1, KnowledgeStatus.ACTIVE);
        long courseId = courseService.createCourse(
                examId, "Architecture Course", "Course description", CourseStatus.PUBLISHED, 1);
        long chapterId = courseService.createChapter(courseId, "Chapter 1", null, 1);
        long videoId = videoService.createVideo(
                chapterId, "Architecture Video", "Video description", "videos/architecture.mp4",
                100, true, VideoStatus.WAITING_REVIEW, 1);
        videoService.linkKnowledge(videoId, knowledgeId);
        videoService.updateStatus(videoId, VideoStatus.PUBLISHED);
        transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.ZERO, BigDecimal.TEN, "hello")));

        profileService.upsert(
                userId, examId, LocalDate.now().plusMonths(2), 30, FoundationLevel.SOME);

        when(storageProvider.generateAccessUrl(eq("videos/architecture.mp4"), any(Duration.class)))
                .thenReturn(URI.create("https://cdn.example.test/videos/architecture.mp4"));

        mockMvc.perform(get("/api/v1/courses").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(courseId))
                .andExpect(jsonPath("$[0].title").value("Architecture Course"));

        mockMvc.perform(get("/api/v1/courses/{id}", courseId).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].videos[0].id").value(videoId));

        mockMvc.perform(get("/api/v1/videos/{id}", videoId).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playUrl").value("https://cdn.example.test/videos/architecture.mp4"))
                .andExpect(jsonPath("$.progressSeconds").value(0))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.transcript[0].text").value("hello"))
                .andExpect(jsonPath("$.knowledgeIds[0]").value(knowledgeId));

        mockMvc.perform(get("/api/v1/knowledge/{id}", knowledgeId).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masteryScore").doesNotExist())
                .andExpect(jsonPath("$.evidenceCount").value(0));

        mockMvc.perform(put("/api/v1/videos/{id}/progress", videoId)
                        .with(user(userId))
                        .contentType("application/json")
                        .content("{\"progressSeconds\":85}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/knowledge/{id}", knowledgeId).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masteryScore").value(3.0))
                .andExpect(jsonPath("$.evidenceCount").value(1));

        mockMvc.perform(get("/api/v1/knowledge/tree").with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(knowledgeId));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
