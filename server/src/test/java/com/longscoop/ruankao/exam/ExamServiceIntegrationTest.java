package com.longscoop.ruankao.exam;

import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.exam.persistence.ExamEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ExamServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Test
    void createsAndLoadsTrimmedExam() {
        long id = examService.create("  architect  ", "  系统架构设计师  ", ExamStatus.ACTIVE);

        ExamEntity exam = examService.findById(id).orElseThrow();

        assertEquals("architect", exam.getCode());
        assertEquals("系统架构设计师", exam.getName());
        assertEquals(ExamStatus.ACTIVE, exam.getStatus());
    }

    @Test
    void listsOnlyActiveExamsInIdOrder() {
        List<ExamEntity> catalog = examService.listActive();
        assertTrue(catalog.stream().anyMatch(exam ->
                exam.getCode().equals("SYSTEM_ARCHITECT_DESIGNER")));
        List<Long> expectedIds = new ArrayList<>(catalog.stream()
                .map(ExamEntity::getId).sorted().toList());
        long first = examService.create("active-1", "Active 1", ExamStatus.ACTIVE);
        examService.create("inactive", "Inactive", ExamStatus.INACTIVE);
        long second = examService.create("active-2", "Active 2", ExamStatus.ACTIVE);
        expectedIds.addAll(List.of(first, second));

        List<ExamEntity> active = examService.listActive();

        assertEquals(expectedIds, active.stream().map(ExamEntity::getId).toList());
        assertTrue(active.stream().noneMatch(exam -> exam.getCode().equals("inactive")));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> examService.create(" ", "Name", ExamStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> examService.create("code", " ", ExamStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> examService.create("code", "Name", null));
    }

    @Test
    void duplicateExamCodeIsRejected() {
        examService.create("duplicate-code", "First", ExamStatus.ACTIVE);

        assertThrows(DataIntegrityViolationException.class,
                () -> examService.create("duplicate-code", "Second", ExamStatus.ACTIVE));
    }
}
