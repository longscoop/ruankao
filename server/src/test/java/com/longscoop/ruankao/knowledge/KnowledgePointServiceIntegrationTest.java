package com.longscoop.ruankao.knowledge;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class KnowledgePointServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Test
    void createsRootAndChildAndListsInStableTreeOrder() {
        long examId = examService.create("kp-tree-exam", "Tree Exam", ExamStatus.ACTIVE);

        long laterRoot = knowledgePointService.create(
                examId, null, 1, "root-b", "Root B", null,
                4, 60.0, 20, 20, KnowledgeStatus.ACTIVE);
        long firstRoot = knowledgePointService.create(
                examId, null, 1, "root-a", "Root A", "description",
                5, 80.0, 30, 10, KnowledgeStatus.ACTIVE);
        long child = knowledgePointService.create(
                examId, firstRoot, 2, "child-a", "Child A", null,
                3, 40.0, 15, 1, KnowledgeStatus.ACTIVE);

        List<KnowledgePointEntity> points = knowledgePointService.listForExam(examId);

        assertEquals(List.of(firstRoot, laterRoot, child),
                points.stream().map(KnowledgePointEntity::getId).toList());
        assertEquals(firstRoot, points.get(2).getParentId());
    }

    @Test
    void rejectsInvalidDomainValues() {
        long examId = examService.create("kp-validation-exam", "Validation", ExamStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 0, "code", "Name", null,
                        3, 50, 10, 0, KnowledgeStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 1, "code", "Name", null,
                        0, 50, 10, 0, KnowledgeStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 1, "code", "Name", null,
                        3, 100.01, 10, 0, KnowledgeStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 1, "code", "Name", null,
                        3, 50, 0, 0, KnowledgeStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 1, " ", "Name", null,
                        3, 50, 10, 0, KnowledgeStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        examId, null, 1, "code", " ", null,
                        3, 50, 10, 0, KnowledgeStatus.ACTIVE));
    }

    @Test
    void rejectsParentFromAnotherExam() {
        long firstExam = examService.create("kp-parent-first", "First", ExamStatus.ACTIVE);
        long secondExam = examService.create("kp-parent-second", "Second", ExamStatus.ACTIVE);
        long parent = knowledgePointService.create(
                firstExam, null, 1, "parent", "Parent", null,
                5, 90, 10, 0, KnowledgeStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () ->
                knowledgePointService.create(
                        secondExam, parent, 2, "child", "Child", null,
                        3, 50, 10, 0, KnowledgeStatus.ACTIVE));
    }

    @Test
    void codeIsUniqueWithinExamButReusableAcrossExams() {
        long firstExam = examService.create("kp-unique-first", "First", ExamStatus.ACTIVE);
        long secondExam = examService.create("kp-unique-second", "Second", ExamStatus.ACTIVE);

        knowledgePointService.create(
                firstExam, null, 1, "same-code", "First KP", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);

        assertThrows(DataIntegrityViolationException.class, () ->
                knowledgePointService.create(
                        firstExam, null, 1, "same-code", "Duplicate", null,
                        3, 50, 10, 0, KnowledgeStatus.ACTIVE));

        knowledgePointService.create(
                secondExam, null, 1, "same-code", "Second KP", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);
    }
}
