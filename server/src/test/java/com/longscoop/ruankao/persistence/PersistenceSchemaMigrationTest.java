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
                "exam", "knowledge_point", "question", "question_knowledge", "answer_record",
                "user_knowledge_mastery", "wrong_question", "user_exam_profile", "assessment_session",
                "study_plan", "study_task", "kb_base", "kb_document", "kb_chunk", "kb_agent",
                "kb_agent_base", "kb_session", "kb_turn"}) {
            assertThat(tableExists(table)).as("table %s exists", table).isTrue();
        }
    }

    @Test
    void flywayAppliesCoreAndKnowledgeAgentMigrations() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(migrationCount).isEqualTo(11);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                Integer.class, tableName);
        return count != null && count == 1;
    }
}
