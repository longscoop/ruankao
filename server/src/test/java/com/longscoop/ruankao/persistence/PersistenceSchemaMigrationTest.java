package com.longscoop.ruankao.persistence;

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
class PersistenceSchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllPhaseTwoTables() {
        Set<String> expected = Set.of(
                "exam",
                "knowledge_point",
                "question",
                "question_knowledge",
                "answer_record",
                "user_knowledge_mastery",
                "wrong_question");

        Set<String> actual = Set.copyOf(jdbcTemplate.queryForList(
                """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'exam',
                    'knowledge_point',
                    'question',
                    'question_knowledge',
                    'answer_record',
                    'user_knowledge_mastery',
                    'wrong_question'
                  )
                """,
                String.class));

        assertEquals(expected, actual);
    }

    @Test
    void databaseRejectsCriticalInvalidRows() {
        String examCode = "schema-constraint-exam";
        jdbcTemplate.update("delete from exam where code = ?", examCode);

        try {
            Long examId = jdbcTemplate.queryForObject(
                    "insert into exam(code, name, status) values (?, ?, ?) returning id",
                    Long.class,
                    examCode,
                    "Schema Constraint Exam",
                    "ACTIVE");

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            "insert into exam(code, name, status) values (?, ?, ?)",
                            examCode,
                            "Duplicate",
                            "ACTIVE"));

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into knowledge_point(
                                exam_id, level, code, name, importance,
                                exam_frequency, estimated_minutes, sort_order, status
                            ) values (?, 1, 'invalid-importance', 'Invalid', 6, 50, 10, 0, 'ACTIVE')
                            """,
                            examId));

            Long knowledgeId = jdbcTemplate.queryForObject(
                    """
                    insert into knowledge_point(
                        exam_id, level, code, name, importance,
                        exam_frequency, estimated_minutes, sort_order, status
                    ) values (?, 1, 'schema-kp', 'Schema KP', 5, 80, 10, 0, 'ACTIVE')
                    returning id
                    """,
                    Long.class,
                    examId);

            Long questionId = jdbcTemplate.queryForObject(
                    """
                    insert into question(
                        exam_id, type, source, status, difficulty, content
                    ) values (?, 'SINGLE_CHOICE', 'REAL_EXAM', 'DRAFT', 'MEDIUM', 'Schema question')
                    returning id
                    """,
                    Long.class,
                    examId);

            assertThrows(DataIntegrityViolationException.class, () ->
                    jdbcTemplate.update(
                            """
                            insert into question_knowledge(question_id, knowledge_id, weight, primary_flag)
                            values (?, ?, 1.2, true)
                            """,
                            questionId,
                            knowledgeId));
        } finally {
            jdbcTemplate.update("delete from exam where code = ?", examCode);
        }
    }
}
