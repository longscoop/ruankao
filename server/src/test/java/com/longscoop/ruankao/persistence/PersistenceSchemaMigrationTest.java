package com.longscoop.ruankao.persistence;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PersistenceSchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void coreSchemaContainsExpectedTables() {
        for (String table : new String[]{
                "exam", "knowledge_point", "question", "question_knowledge",
                "answer_record", "user_knowledge_mastery", "wrong_question"
        }) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                    Integer.class,
                    table);
            assertThat(count).as(table).isEqualTo(1);
        }
    }

    @Test
    void flywayAppliedCoreLearningSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(migrationCount).isEqualTo(11);
    }

    @Test
    void coreSchemaContainsAnswerHistorySafeguards() {
        assertThat(columnExists("answer_record", "confidence")).isTrue();
        assertThat(columnExists("answer_record", "source")).isTrue();
        assertThat(columnExists("answer_record", "mastery_applied")).isTrue();
        assertThat(indexExists("user_knowledge_mastery_pkey")).isTrue();
        assertThat(indexExists("idx_answer_user_answered")).isTrue();
    }

    @Test
    void databaseRejectsCriticalInvalidRows() {
        String examCode = "schema-constraint-exam";
        jdbcTemplate.update("delete from exam where code = ?", examCode);

        try {
            Long examId = jdbcTemplate.queryForObject(
                    "insert into exam(code, name, status) values (?, ?, ?) returning id",
                    Long.class, examCode, "Schema Constraint Exam", "ACTIVE");

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into exam(code, name, status) values (?, ?, ?)",
                    examCode, "Duplicate", "ACTIVE"))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    insert into knowledge_point(
                        exam_id, level, code, name, importance,
                        exam_frequency, estimated_minutes, sort_order, status
                    ) values (?, 1, 'invalid-importance', 'Invalid', 6, 50, 10, 0, 'ACTIVE')
                    """, examId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            Long knowledgeId = jdbcTemplate.queryForObject(
                    """
                    insert into knowledge_point(
                        exam_id, level, code, name, importance,
                        exam_frequency, estimated_minutes, sort_order, status
                    ) values (?, 1, 'schema-kp', 'Schema KP', 5, 80, 10, 0, 'ACTIVE')
                    returning id
                    """, Long.class, examId);

            Long questionId = jdbcTemplate.queryForObject(
                    """
                    insert into question(
                        exam_id, type, source, status, difficulty, content
                    ) values (?, 'SINGLE_CHOICE', 'REAL_EXAM', 'DRAFT', 'MEDIUM', 'Schema question')
                    returning id
                    """, Long.class, examId);

            assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    insert into question_knowledge(question_id, knowledge_id, weight, primary_flag)
                    values (?, ?, 1.2, true)
                    """, questionId, knowledgeId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("delete from exam where code = ?", examCode);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = ? and column_name = ?",
                Integer.class, table, column);
        return count != null && count == 1;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pg_indexes where schemaname = 'public' and indexname = ?",
                Integer.class, indexName);
        return count != null && count == 1;
    }
}
