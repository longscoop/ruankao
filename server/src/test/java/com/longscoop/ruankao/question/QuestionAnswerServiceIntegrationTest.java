package com.longscoop.ruankao.question;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.learning.persistence.MasteryDelta;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.model.WrongQuestionStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class QuestionAnswerServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionAnswerService questionAnswerService;

    @Autowired
    private MasteryApplicationService masteryApplicationService;

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Test
    void serverComputesCorrectnessAndUpdatesWeightedMastery() {
        Fixture fixture = fixture("answer-correct", 0.7, 0.3);

        QuestionAnswerResult result = questionAnswerService.submit(
                701L,
                fixture.questionId(),
                UUID.randomUUID(),
                "answer-correct-key",
                "B",
                15,
                AnswerConfidence.CONFIDENT,
                AnswerSource.DAILY_PLAN);

        assertTrue(result.correct());
        assertEquals("B", result.correctAnswer());
        assertTrue(masteryApplicationService.findAnswer(result.answerId()).orElseThrow().getCorrect());

        UserKnowledgeMasteryEntity primary = masteryApplicationService
                .findMastery(701L, fixture.primaryKnowledgeId()).orElseThrow();
        UserKnowledgeMasteryEntity secondary = masteryApplicationService
                .findMastery(701L, fixture.secondaryKnowledgeId()).orElseThrow();

        assertEquals(3.5, primary.getMasteryScore(), 0.001);
        assertEquals(1.5, secondary.getMasteryScore(), 0.001);
        assertEquals(1, primary.getEvidenceCount());
        assertEquals(1, secondary.getEvidenceCount());
    }

    @Test
    void wrongAnswerCreatesActiveWrongQuestion() {
        Fixture fixture = fixture("answer-wrong", 1.0, 0.0);

        QuestionAnswerResult result = questionAnswerService.submit(
                702L,
                fixture.questionId(),
                UUID.randomUUID(),
                "answer-wrong-key",
                "A",
                12,
                AnswerConfidence.CONFIDENT,
                AnswerSource.CHAPTER);

        assertFalse(result.correct());
        assertEquals(
                WrongQuestionStatus.ACTIVE,
                wrongQuestionService.find(702L, fixture.questionId()).orElseThrow().getStatus());
        assertEquals(
                1,
                masteryApplicationService.findMastery(702L, fixture.primaryKnowledgeId())
                        .orElseThrow().getEvidenceCount());
    }

    @Test
    void duplicateIdempotencyKeyReturnsSameAnswerWithoutApplyingEffectsTwice() {
        Fixture fixture = fixture("answer-idempotent", 1.0, 0.0);

        QuestionAnswerResult first = questionAnswerService.submit(
                703L,
                fixture.questionId(),
                UUID.randomUUID(),
                "same-key",
                "B",
                10,
                AnswerConfidence.CONFIDENT,
                AnswerSource.DAILY_PLAN);

        QuestionAnswerResult second = questionAnswerService.submit(
                703L,
                fixture.questionId(),
                UUID.randomUUID(),
                "same-key",
                "A",
                99,
                AnswerConfidence.GUESS,
                AnswerSource.DAILY_PLAN);

        assertEquals(first.answerId(), second.answerId());
        assertEquals(first.correct(), second.correct());
        assertEquals(
                1,
                masteryApplicationService.findMastery(703L, fixture.primaryKnowledgeId())
                        .orElseThrow().getEvidenceCount());
        assertFalse(wrongQuestionService.find(703L, fixture.questionId()).isPresent());
    }

    @Test
    void correctWrongReviewAdvancesPersistentWrongQuestionLifecycle() {
        Fixture fixture = fixture("answer-review", 1.0, 0.0);
        UUID seedAnswer = masteryApplicationService.recordAnswer(
                704L,
                fixture.questionId(),
                UUID.randomUUID(),
                "B",
                true,
                10,
                null,
                AnswerSource.DAILY_PLAN);
        masteryApplicationService.apply(
                seedAnswer,
                List.of(new MasteryDelta(fixture.primaryKnowledgeId(), 80.0, true)));
        wrongQuestionService.recordWrong(704L, fixture.questionId());

        questionAnswerService.submit(
                704L,
                fixture.questionId(),
                UUID.randomUUID(),
                "review-1",
                "B",
                10,
                AnswerConfidence.CONFIDENT,
                AnswerSource.WRONG_REVIEW);
        assertEquals(
                WrongQuestionStatus.ACTIVE,
                wrongQuestionService.find(704L, fixture.questionId()).orElseThrow().getStatus());

        questionAnswerService.submit(
                704L,
                fixture.questionId(),
                UUID.randomUUID(),
                "review-2",
                "B",
                10,
                AnswerConfidence.CONFIDENT,
                AnswerSource.WRONG_REVIEW);
        assertEquals(
                WrongQuestionStatus.MASTERED,
                wrongQuestionService.find(704L, fixture.questionId()).orElseThrow().getStatus());
    }

    @Test
    void rejectsMissingIdempotencyKeyAndUnpublishedQuestion() {
        Fixture fixture = fixture("answer-validation", 1.0, 0.0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                questionAnswerService.submit(
                        705L,
                        fixture.questionId(),
                        UUID.randomUUID(),
                        " ",
                        "B",
                        10,
                        null,
                        AnswerSource.DAILY_PLAN));

        long draftQuestion = questionService.create(
                fixture.examId(),
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                "Draft",
                "B",
                List.of(new QuestionKnowledgeLink(fixture.primaryKnowledgeId(), 1.0, true)));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                questionAnswerService.submit(
                        705L,
                        draftQuestion,
                        UUID.randomUUID(),
                        "draft-key",
                        "B",
                        10,
                        null,
                        AnswerSource.DAILY_PLAN));
    }

    private Fixture fixture(String prefix, double primaryWeight, double secondaryWeight) {
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long primary = knowledgePointService.create(
                examId, null, 1, prefix + "-primary", "Primary", null,
                5, 90, 10, 1, KnowledgeStatus.ACTIVE);
        long secondary = knowledgePointService.create(
                examId, null, 1, prefix + "-secondary", "Secondary", null,
                3, 50, 10, 2, KnowledgeStatus.ACTIVE);

        List<QuestionKnowledgeLink> links = secondaryWeight > 0
                ? List.of(
                        new QuestionKnowledgeLink(primary, primaryWeight, true),
                        new QuestionKnowledgeLink(secondary, secondaryWeight, false))
                : List.of(new QuestionKnowledgeLink(primary, 1.0, true));

        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Choose B",
                "B",
                links);

        return new Fixture(examId, questionId, primary, secondary);
    }

    private record Fixture(
            long examId,
            long questionId,
            long primaryKnowledgeId,
            long secondaryKnowledgeId) {
    }
}
