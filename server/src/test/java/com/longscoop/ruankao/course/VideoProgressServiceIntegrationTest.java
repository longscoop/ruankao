package com.longscoop.ruankao.course;

import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.course.persistence.UserVideoProgressEntity;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class VideoProgressServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired private ExamService examService;
    @Autowired private CourseService courseService;
    @Autowired private KnowledgePointService knowledgePointService;
    @Autowired private VideoService videoService;
    @Autowired private VideoProgressService progressService;
    @Autowired private MasteryApplicationService masteryApplicationService;

    @Test
    void completesAtEightyFivePercentAndAppliesWeakEvidenceOnlyOnce() {
        Fixture fixture = createFixture();

        UserVideoProgressEntity belowThreshold = progressService.update(10L, fixture.videoId(), 8499);
        UserVideoProgressEntity completed = progressService.update(10L, fixture.videoId(), 8500);
        UserVideoProgressEntity repeated = progressService.update(10L, fixture.videoId(), 10000);
        UserKnowledgeMasteryEntity mastery = masteryApplicationService.findMastery(10L, fixture.knowledgeId()).orElseThrow();

        assertFalse(belowThreshold.getCompleted());
        assertEquals(84.99, belowThreshold.getCompletionRate());
        assertTrue(completed.getCompleted());
        assertTrue(completed.getMasteryApplied());
        assertTrue(repeated.getMasteryApplied());
        assertEquals(3.0, mastery.getMasteryScore());
        assertEquals(1, mastery.getEvidenceCount());
        assertEquals(0, mastery.getCorrectStreak());
        assertEquals(0, mastery.getWrongStreak());
    }

    @Test
    void neverMovesProgressBackwardsAndCapsItAtVideoDuration() {
        Fixture fixture = createFixture();

        progressService.update(20L, fixture.videoId(), 9000);
        UserVideoProgressEntity afterBackwardReport = progressService.update(20L, fixture.videoId(), 100);
        UserVideoProgressEntity capped = progressService.update(20L, fixture.videoId(), 20000);

        assertEquals(9000, afterBackwardReport.getProgressSeconds());
        assertEquals(10000, capped.getProgressSeconds());
        assertEquals(100.0, capped.getCompletionRate());
    }

    @Test
    void rejectsNegativeProgress() {
        Fixture fixture = createFixture();

        assertThrows(IllegalArgumentException.class, () -> progressService.update(30L, fixture.videoId(), -1));
    }

    private Fixture createFixture() {
        long examId = examService.create("progress-exam", "Progress exam", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(examId, "Course", null, CourseStatus.DRAFT, 0);
        long chapterId = courseService.createChapter(courseId, "Chapter", null, 0);
        long knowledgeId = knowledgePointService.create(examId, null, 1, "progress-knowledge", "Knowledge", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);
        long videoId = videoService.createVideo(chapterId, "Video", null, "course/video.mp4", 10000,
                false, VideoStatus.DRAFT, 0);
        videoService.linkKnowledge(videoId, knowledgeId);
        return new Fixture(videoId, knowledgeId);
    }

    private record Fixture(long videoId, long knowledgeId) { }
}
