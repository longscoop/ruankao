package com.longscoop.ruankao.ai.knowledge;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/** Adds knowledge migration coverage without replacing existing core constraint tests. */
@SpringBootTest
class KnowledgeSchemaMigrationTest extends PostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test void createsAllKnowledgeAgentTables() {
        for (String table : new String[]{"kb_base", "kb_document", "kb_chunk", "kb_agent", "kb_agent_base", "kb_session", "kb_turn"}) {
            assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name=?", Integer.class, table))
                    .as("knowledge table %s", table).isEqualTo(1);
        }
    }

    @Test void appliesV11ExactlyOnce() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success and version='11'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where not success", Integer.class)).isZero();
    }
}
