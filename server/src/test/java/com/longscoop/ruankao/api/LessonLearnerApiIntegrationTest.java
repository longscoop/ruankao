package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.content.model.LessonBlockType;
import com.longscoop.ruankao.content.model.LessonStatus;
import com.longscoop.ruankao.content.persistence.*;
import com.longscoop.ruankao.course.CourseService;
import com.longscoop.ruankao.course.model.CourseStatus;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LessonLearnerApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired CourseService courseService;
    @Autowired UserExamProfileService profileService;
    @Autowired LessonMapper lessonMapper;
    @Autowired LessonBlockMapper lessonBlockMapper;
    @Autowired LessonKnowledgeMapper lessonKnowledgeMapper;

    @MockBean StorageProvider storageProvider;

    @Test
    void publishedLessonAppearsInCourseAndReturnsOrderedBlocks() throws Exception {
        long userId=8101L;
        long examId=examService.create("lesson-exam","System Architect", ExamStatus.ACTIVE);
        long knowledgeId=knowledgePointService.create(
                examId,null,1,"lambda","Lambda Architecture",null,
                5,80,20,1, KnowledgeStatus.ACTIVE);
        long courseId=courseService.createCourse(
                examId,"Big Data Architecture",null, CourseStatus.PUBLISHED,1);
        long chapterId=courseService.createChapter(courseId,"Lambda and Kappa",null,1);

        LessonEntity lesson=new LessonEntity();
        lesson.setChapterId(chapterId);
        lesson.setTitle("Lambda Architecture Design");
        lesson.setSummary("Source lecture");
        lesson.setStatus(LessonStatus.PUBLISHED);
        lesson.setSortOrder(1);
        lessonMapper.insert(lesson);

        LessonBlockEntity text=new LessonBlockEntity();
        text.setLessonId(lesson.getId()); text.setBlockType(LessonBlockType.TEXT);
        text.setTextContent("Batch layer stores the immutable master dataset.");
        text.setSourcePage(10); text.setSortOrder(0); lessonBlockMapper.insert(text);

        LessonBlockEntity image=new LessonBlockEntity();
        image.setLessonId(lesson.getId()); image.setBlockType(LessonBlockType.DIAGRAM);
        image.setImageObjectKey("content-imports/x/pages/10.png");
        image.setSourcePage(10); image.setSortOrder(1); lessonBlockMapper.insert(image);

        LessonKnowledgeEntity relation=new LessonKnowledgeEntity();
        relation.setLessonId(lesson.getId()); relation.setKnowledgeId(knowledgeId);
        lessonKnowledgeMapper.insert(relation);

        profileService.upsert(
                userId,examId, LocalDate.now().plusMonths(2),30, FoundationLevel.SOME);

        when(storageProvider.generateAccessUrl(
                eq("content-imports/x/pages/10.png"), any(Duration.class)))
                .thenReturn(URI.create("https://cdn.example.test/pages/10.png"));

        mockMvc.perform(get("/api/v1/courses/{id}",courseId).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessons[0].id").value(lesson.getId()))
                .andExpect(jsonPath("$.chapters[0].lessons[0].title").value("Lambda Architecture Design"));

        mockMvc.perform(get("/api/v1/lessons/{id}",lesson.getId()).with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].blockType").value("TEXT"))
                .andExpect(jsonPath("$.blocks[0].textContent").value(
                        "Batch layer stores the immutable master dataset."))
                .andExpect(jsonPath("$.blocks[1].imageUrl").value(
                        "https://cdn.example.test/pages/10.png"))
                .andExpect(jsonPath("$.knowledgeIds[0]").value(knowledgeId));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId){
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),"n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
