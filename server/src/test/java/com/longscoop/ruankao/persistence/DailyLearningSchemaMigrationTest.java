package com.longscoop.ruankao.persistence;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DailyLearningSchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsDailyLearningWorkflowTables() {
        Set<String> expected = Set.of(
                "user_exam_profile",
                "assessment_session",
                "assessment_item",
                "study_plan",
                "study_task");

        Set<String> actual = Set.copyOf(jdbcTemplate.queryForList(
                """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'user_exam_profile',
                    'assessment_session',
                    'assessment_item',
                    'study_plan',
                    'study_task'
                  )
                """,
                String.class));

        assertEquals(expected, actual);
    }

    @Test
    void enforcesProfileAndPlanUniquenessAndTargetMinutes() {
        String examCode = "phase3-schema-exam";
        jdbcTemplate.update("delete from exam where code = ?", examCode);

        try {
            Long examId = jdbcTemplate.queryForObject(
                    "insert into exam(code, name, status) values (?, ?, 'ACTIVE') returning id",
                    Long.class,
                    examCode,
                    "Phase 3 Schema Exam");

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into user_exam_profile(
                                user_id, exam_id, exam_date, daily_target_minutes,
                                foundation_level, assessment_completed
                            ) values (?, ?, ?, 20, 'SOME', false)
                            """,
                            9001L,
                            examId,
                            LocalDate.now().plusDays(30)));

            jdbcTemplate.update(
                    """
                    insert into user_exam_profile(
                        user_id, exam_id, exam_date, daily_target_minutes,
                        foundation_level, assessment_completed
                    ) values (?, ?, ?, 30, 'SOME', false)
                    """,
                    9001L,
                    examId,
                    LocalDate.now().plusDays(30));

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into user_exam_profile(
                                user_id, exam_id, exam_date, daily_target_minutes,
                                foundation_level, assessment_completed
                            ) values (?, ?, ?, 30, 'ZERO', false)
                            """,
                            9001L,
                            examId,
                            LocalDate.now().plusDays(40)));

            jdbcTemplate.update(
                    """
                    insert into study_plan(
                        user_id, exam_id, plan_date, target_minutes,
                        estimated_minutes, actual_minutes, completion_rate, status
                    ) values (?, ?, ?, 30, 30, 0, 0, 'ACTIVE')
                    """,
                    9001L,
                    examId,
                    LocalDate.now());

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into study_plan(
                                user_id, exam_id, plan_date, target_minutes,
                                estimated_minutes, actual_minutes, completion_rate, status
                            ) values (?, ?, ?, 30, 30, 0, 0, 'ACTIVE')
                            """,
                            9001L,
                            examId,
                            LocalDate.now()));

            UUID assessmentId = UUID.randomUUID();
            jdbcTemplate.update(
                    """
                    insert into assessment_session(id, user_id, exam_id, status)
                    values (?, ?, ?, 'IN_PROGRESS')
                    """,
                    assessmentId,
                    9001L,
                    examId);
        } finally {
            jdbcTemplate.update("delete from assessment_session where user_id = ?", 9001L);
            jdbcTemplate.update("delete from study_plan where user_id = ?", 9001L);
            jdbcTemplate.update("delete from user_exam_profile where user_id = ?", 9001L);
            jdbcTemplate.update("delete from exam where code = ?", examCode);
        }
    }
}
