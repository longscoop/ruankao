package com.longscoop.ruankao.persistence;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(indexExists("uk_user_knowledge_mastery_user_knowledge")).isTrue();
        assertThat(indexExists("idx_answer_record_user_knowledge_time")).isTrue();
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
