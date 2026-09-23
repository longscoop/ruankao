package com.longscoop.ruankao.course;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CourseVideoSchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllCourseVideoTables() {
        Set<String> expected = Set.of(
                "course",
                "course_chapter",
                "video",
                "video_knowledge_relation",
                "user_video_progress",
                "video_transcript_segment");

        Set<String> actual = Set.copyOf(jdbcTemplate.queryForList(
                """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'course',
                    'course_chapter',
                    'video',
                    'video_knowledge_relation',
                    'user_video_progress',
                    'video_transcript_segment'
                  )
                """,
                String.class));

        assertEquals(expected, actual);
    }

    @Test
    void rejectsCriticalInvalidVideoRows() {
        String code = "video-schema-exam";
        jdbcTemplate.update("delete from exam where code = ?", code);

        try {
            Long examId = jdbcTemplate.queryForObject(
                    "insert into exam(code, name, status) values (?, ?, 'ACTIVE') returning id",
                    Long.class,
                    code,
                    "Video Schema");

            Long knowledgeId = jdbcTemplate.queryForObject(
                    """
                    insert into knowledge_point(
                        exam_id, level, code, name, importance,
                        exam_frequency, estimated_minutes, sort_order, status
                    ) values (?, 1, 'video-schema-kp', 'Video KP', 3, 50, 10, 0, 'ACTIVE')
                    returning id
                    """,
                    Long.class,
                    examId);

            Long courseId = jdbcTemplate.queryForObject(
                    """
                    insert into course(exam_id, title, status, sort_order)
                    values (?, 'Course', 'DRAFT', 0)
                    returning id
                    """,
                    Long.class,
                    examId);

            Long chapterId = jdbcTemplate.queryForObject(
                    """
                    insert into course_chapter(course_id, title, sort_order)
                    values (?, 'Chapter', 0)
                    returning id
                    """,
                    Long.class,
                    courseId);

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into video(
                                chapter_id, title, object_key, duration_seconds,
                                free_flag, status, sort_order
                            ) values (?, 'Bad', 'bad.mp4', 0, false, 'DRAFT', 0)
                            """,
                            chapterId));

            Long videoId = jdbcTemplate.queryForObject(
                    """
                    insert into video(
                        chapter_id, title, object_key, duration_seconds,
                        free_flag, status, sort_order
                    ) values (?, 'Video', 'video.mp4', 100, false, 'DRAFT', 0)
                    returning id
                    """,
                    Long.class,
                    chapterId);

            jdbcTemplate.update(
                    """
                    insert into video_knowledge_relation(video_id, knowledge_id)
                    values (?, ?)
                    """,
                    videoId,
                    knowledgeId);

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into user_video_progress(
                                user_id, video_id, progress_seconds,
                                completion_rate, completed, mastery_applied
                            ) values (1, ?, 10, 100.01, false, false)
                            """,
                            videoId));

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into video_transcript_segment(
                                video_id, start_second, end_second, content
                            ) values (?, 10, 10, 'invalid')
                            """,
                            videoId));
        } finally {
            jdbcTemplate.update("delete from exam where code = ?", code);
        }
    }
}
