package com.longscoop.ruankao.exam;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamCatalogMigrationTest {

    @Test
    void v1CatalogSeedsTheSystemArchitectExam() throws IOException {
        try (var migration = getClass().getResourceAsStream(
                "/db/migration/V11__seed_system_architect_exam.sql")) {
            assertNotNull(migration, "the V1 exam catalog migration must exist");
            String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("insert into exam"));
            assertTrue(sql.contains("'SYSTEM_ARCHITECT_DESIGNER'"));
            assertTrue(sql.contains("系统架构设计师"));
            assertTrue(sql.contains("'ACTIVE'"));
            assertTrue(sql.contains("on conflict (code) do nothing"));
        }
    }
}
