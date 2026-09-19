package com.longscoop.ruankao.persistence;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
