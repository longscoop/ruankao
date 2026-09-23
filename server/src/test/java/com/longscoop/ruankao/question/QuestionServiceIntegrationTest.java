package com.longscoop.ruankao.question;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class QuestionServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Test
    void createsQuestionAndKnowledgeLinksAtomically() {
        long examId = examService.create("question-create-exam", "Question Exam", ExamStatus.ACTIVE);
        long primaryKnowledge = createKnowledge(examId, "architecture");
        long secondaryKnowledge = createKnowledge(examId, "reliability");

        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                "  Which architecture statement is correct?  ",
                List.of(
                        new QuestionKnowledgeLink(primaryKnowledge, 0.7, true),
                        new QuestionKnowledgeLink(secondaryKnowledge, 0.3, false)));

        QuestionEntity question = questionService.findById(questionId).orElseThrow();
        List<QuestionKnowledgeEntity> links = questionService.listKnowledgeLinks(questionId);

        assertEquals("Which architecture statement is correct?", question.getContent());
        assertEquals(QuestionSource.REAL_EXAM, question.getSource());
        assertEquals(2, links.size());
        assertEquals(primaryKnowledge, links.get(0).getKnowledgeId());
        assertEquals(0.7, links.get(0).getWeight(), 0.000001);
        assertEquals(true, links.get(0).getPrimaryFlag());
    }

    @Test
    void preservesAiGeneratedSourceAsDistinctFromRealExam() {
        long examId = examService.create("question-ai-exam", "AI Exam", ExamStatus.ACTIVE);
        long knowledgeId = createKnowledge(examId, "ai-kp");

        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.AI_GENERATED,
                QuestionStatus.DRAFT,
                QuestionDifficulty.EASY,
                "AI practice question",
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));

        assertEquals(QuestionSource.AI_GENERATED, questionService.findById(questionId).orElseThrow().getSource());
    }

    @Test
    void rejectsInvalidKnowledgeLinkSets() {
        long examId = examService.create("question-validation-exam", "Validation", ExamStatus.ACTIVE);
        long first = createKnowledge(examId, "first");
        long second = createKnowledge(examId, "second");

        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, " ",
                List.of(new QuestionKnowledgeLink(first, 1.0, true))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Empty links", List.of()));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Zero weight",
                List.of(new QuestionKnowledgeLink(first, 0.0, true))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Too much weight",
                List.of(new QuestionKnowledgeLink(first, 1.01, true))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Bad sum",
                List.of(
                        new QuestionKnowledgeLink(first, 0.6, true),
                        new QuestionKnowledgeLink(second, 0.3, false))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "No primary",
                List.of(
                        new QuestionKnowledgeLink(first, 0.5, false),
                        new QuestionKnowledgeLink(second, 0.5, false))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Two primary",
                List.of(
                        new QuestionKnowledgeLink(first, 0.5, true),
                        new QuestionKnowledgeLink(second, 0.5, true))));
        assertThrows(IllegalArgumentException.class, () -> createQuestion(examId, "Duplicate knowledge",
                List.of(
                        new QuestionKnowledgeLink(first, 0.5, true),
                        new QuestionKnowledgeLink(first, 0.5, false))));
    }

    @Test
    void rejectsKnowledgeFromAnotherExam() {
        long firstExam = examService.create("question-cross-first", "First", ExamStatus.ACTIVE);
        long secondExam = examService.create("question-cross-second", "Second", ExamStatus.ACTIVE);
        long foreignKnowledge = createKnowledge(secondExam, "foreign");

        assertThrows(IllegalArgumentException.class, () ->
                createQuestion(firstExam, "Cross exam",
                        List.of(new QuestionKnowledgeLink(foreignKnowledge, 1.0, true))));
    }

    @Test
    void rejectsMissingRequiredEnums() {
        long examId = examService.create("question-enum-exam", "Enum", ExamStatus.ACTIVE);
        long knowledgeId = createKnowledge(examId, "enum-kp");
        List<QuestionKnowledgeLink> links = List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true));

        assertThrows(IllegalArgumentException.class, () ->
                questionService.create(examId, null, QuestionSource.REAL_EXAM, QuestionStatus.DRAFT,
                        QuestionDifficulty.MEDIUM, "Question", links));
        assertThrows(IllegalArgumentException.class, () ->
                questionService.create(examId, QuestionType.SINGLE_CHOICE, null, QuestionStatus.DRAFT,
                        QuestionDifficulty.MEDIUM, "Question", links));
        assertThrows(IllegalArgumentException.class, () ->
                questionService.create(examId, QuestionType.SINGLE_CHOICE, QuestionSource.REAL_EXAM, null,
                        QuestionDifficulty.MEDIUM, "Question", links));
        assertThrows(IllegalArgumentException.class, () ->
                questionService.create(examId, QuestionType.SINGLE_CHOICE, QuestionSource.REAL_EXAM,
                        QuestionStatus.DRAFT, null, "Question", links));
    }

    private long createKnowledge(long examId, String code) {
        return knowledgePointService.create(
                examId, null, 1, code, code, null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);
    }

    private long createQuestion(long examId, String content, List<QuestionKnowledgeLink> links) {
        return questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                content,
                links);
    }
}
