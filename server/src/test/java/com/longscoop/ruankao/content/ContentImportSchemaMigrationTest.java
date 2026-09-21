package com.longscoop.ruankao.content;

import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ContentImportSchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsLessonAndImportWorkflowTables() {
        Set<String> expected = Set.of(
                "lesson",
                "lesson_block",
                "lesson_knowledge",
                "content_import_batch",
                "content_import_page",
                "content_import_item",
                "content_import_issue");

        Set<String> actual = Set.copyOf(jdbcTemplate.queryForList(
                """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'lesson',
                    'lesson_block',
                    'lesson_knowledge',
                    'content_import_batch',
                    'content_import_page',
                    'content_import_item',
                    'content_import_issue'
                  )
                """,
                String.class));

        assertEquals(expected, actual);
    }

    @Test
    void appUserHasRoleAndDefaultsToUser() {
        String role = jdbcTemplate.queryForObject(
                """
                insert into app_user(display_name)
                values ('schema-role-test')
                returning role
                """,
                String.class);

        assertEquals("USER", role);
    }
}
