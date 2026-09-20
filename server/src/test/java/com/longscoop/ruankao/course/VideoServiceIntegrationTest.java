package com.longscoop.ruankao.course;

import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.course.persistence.VideoEntity;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class VideoServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private VideoService videoService;

    @Test
    void createsTrimmedVideoAndListsItByChapterAndKnowledgePoint() {
        CourseFixture fixture = createCourseFixture("video-list");
        long first = videoService.createVideo(
                fixture.chapterId(), "  First video  ", "  introduction  ",
                "  course/first.mp4  ", 300, false, VideoStatus.DRAFT, 10);
        long later = videoService.createVideo(
                fixture.chapterId(), "Later video", null,
                "course/later.mp4", 120, true, VideoStatus.DRAFT, 20);

        videoService.linkKnowledge(first, fixture.knowledgeId());

        List<VideoEntity> chapterVideos = videoService.listForChapter(fixture.chapterId());
        List<VideoEntity> knowledgeVideos = videoService.listForKnowledgePoint(fixture.knowledgeId());

        assertEquals(List.of(first, later), chapterVideos.stream().map(VideoEntity::getId).toList());
        assertEquals("First video", chapterVideos.get(0).getTitle());
        assertEquals("introduction", chapterVideos.get(0).getDescription());
        assertEquals("course/first.mp4", chapterVideos.get(0).getObjectKey());
        assertEquals(List.of(first), knowledgeVideos.stream().map(VideoEntity::getId).toList());
    }

    @Test
    void allowsPublishingOnlyAfterKnowledgeIsLinked() {
        CourseFixture fixture = createCourseFixture("video-publish");
        long videoId = videoService.createVideo(
                fixture.chapterId(), "Video", null, "course/video.mp4",
                300, false, VideoStatus.DRAFT, 0);

        assertThrows(IllegalStateException.class, () ->
                videoService.updateStatus(videoId, VideoStatus.PUBLISHED));

        videoService.linkKnowledge(videoId, fixture.knowledgeId());
        videoService.updateStatus(videoId, VideoStatus.PUBLISHED);

        assertEquals(VideoStatus.PUBLISHED, videoService.find(videoId).getStatus());
    }

    @Test
    void rejectsInvalidMetadataAndKnowledgeLinksOutsideCourseExam() {
        CourseFixture fixture = createCourseFixture("video-validation");
        long otherExamId = examService.create("video-other-exam", "Other", ExamStatus.ACTIVE);
        long foreignKnowledgeId = knowledgePointService.create(
                otherExamId, null, 1, "foreign", "Foreign", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);
        long videoId = videoService.createVideo(
                fixture.chapterId(), "Video", null, "course/video.mp4",
                300, false, VideoStatus.DRAFT, 0);

        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(fixture.chapterId(), " ", null, "key", 1, false, VideoStatus.DRAFT, 0));
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(fixture.chapterId(), "Video", null, " ", 1, false, VideoStatus.DRAFT, 0));
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(fixture.chapterId(), "Video", null, "key", 0, false, VideoStatus.DRAFT, 0));
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(fixture.chapterId(), "Video", null, "key", 1, false, VideoStatus.DRAFT, -1));
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(999999L, "Video", null, "key", 1, false, VideoStatus.DRAFT, 0));
        assertThrows(IllegalArgumentException.class, () -> videoService.linkKnowledge(videoId, foreignKnowledgeId));

        videoService.linkKnowledge(videoId, fixture.knowledgeId());
        assertThrows(DataIntegrityViolationException.class, () ->
                videoService.linkKnowledge(videoId, fixture.knowledgeId()));
    }

    private CourseFixture createCourseFixture(String code) {
        long examId = examService.create(code + "-exam", "Video exam", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(examId, "Course", null, CourseStatus.DRAFT, 0);
        long chapterId = courseService.createChapter(courseId, "Chapter", null, 0);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, code + "-knowledge", "Knowledge", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);
        return new CourseFixture(chapterId, knowledgeId);
    }

    private record CourseFixture(long chapterId, long knowledgeId) {
    }
}
