package com.longscoop.ruankao.course;

import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.course.persistence.VideoTranscriptSegmentEntity;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class VideoTranscriptServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoTranscriptService transcriptService;

    @Test
    void replacesSegmentsAndReturnsThemInStartTimeOrder() {
        long videoId = createVideo();

        transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.valueOf(10), BigDecimal.valueOf(12), "Second"),
                new VideoTranscriptService.Segment(BigDecimal.ZERO, BigDecimal.valueOf(3), "First")));

        List<VideoTranscriptSegmentEntity> segments = transcriptService.list(videoId);

        assertEquals(List.of("First", "Second"),
                segments.stream().map(VideoTranscriptSegmentEntity::getContent).toList());
        assertEquals(0, segments.get(0).getStartSecond().compareTo(BigDecimal.ZERO));
    }

    @Test
    void rejectsInvalidSegmentsWithoutReplacingExistingTranscript() {
        long videoId = createVideo();
        transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.ZERO, BigDecimal.ONE, "Original")));

        assertThrows(IllegalArgumentException.class, () -> transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.valueOf(-1), BigDecimal.ONE, "Negative"))));
        assertThrows(IllegalArgumentException.class, () -> transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.ONE, BigDecimal.ONE, "Zero length"))));
        assertThrows(IllegalArgumentException.class, () -> transcriptService.replace(videoId, List.of(
                new VideoTranscriptService.Segment(BigDecimal.ONE, BigDecimal.TEN, " "))));

        assertEquals(List.of("Original"), transcriptService.list(videoId).stream()
                .map(VideoTranscriptSegmentEntity::getContent).toList());
    }

    private long createVideo() {
        long examId = examService.create("transcript-exam", "Transcript exam", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(examId, "Course", null, CourseStatus.DRAFT, 0);
        long chapterId = courseService.createChapter(courseId, "Chapter", null, 0);
        return videoService.createVideo(chapterId, "Video", null, "course/video.mp4", 60,
                false, VideoStatus.DRAFT, 0);
    }
}
