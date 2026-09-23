package com.longscoop.ruankao.learning.persistence;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MasteryApplicationServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private MasteryApplicationService masteryApplicationService;

    @Test
    void persistsAnswerEvidenceBeforeMasteryIsApplied() {
        Fixture fixture = fixture("answer-record");
        UUID sessionId = UUID.randomUUID();

        UUID answerId = masteryApplicationService.recordAnswer(
                101L,
                fixture.questionId(),
                sessionId,
                "B",
                true,
                18,
                AnswerConfidence.CONFIDENT,
                AnswerSource.CHAPTER);

        AnswerRecordEntity answer = masteryApplicationService.findAnswer(answerId).orElseThrow();

        assertEquals(101L, answer.getUserId());
        assertEquals(fixture.questionId(), answer.getQuestionId());
        assertEquals(sessionId, answer.getSessionId());
        assertEquals("B", answer.getAnswer());
        assertEquals(true, answer.getCorrect());
        assertEquals(18, answer.getDurationSeconds());
        assertEquals(AnswerConfidence.CONFIDENT, answer.getConfidence());
        assertEquals(AnswerSource.CHAPTER, answer.getSource());
        assertFalse(answer.getMasteryApplied());
        assertNotNull(answer.getAnsweredAt());
    }

    @Test
    void appliesMultipleKnowledgeDeltasExactlyOnce() {
        Fixture fixture = fixture("mastery-once");
        UUID answerId = recordAnswer(202L, fixture.questionId(), true);

        boolean first = masteryApplicationService.apply(
                answerId,
                List.of(
                        new MasteryDelta(fixture.primaryKnowledgeId(), 12.5, true),
                        new MasteryDelta(fixture.secondaryKnowledgeId(), -8.0, false)));

        boolean second = masteryApplicationService.apply(
                answerId,
                List.of(
                        new MasteryDelta(fixture.primaryKnowledgeId(), 50.0, true),
                        new MasteryDelta(fixture.secondaryKnowledgeId(), 50.0, true)));

        assertTrue(first);
        assertFalse(second);

        UserKnowledgeMasteryEntity primary = masteryApplicationService
                .findMastery(202L, fixture.primaryKnowledgeId()).orElseThrow();
        UserKnowledgeMasteryEntity secondary = masteryApplicationService
                .findMastery(202L, fixture.secondaryKnowledgeId()).orElseThrow();

        assertEquals(12.5, primary.getMasteryScore(), 0.001);
        assertEquals(1, primary.getEvidenceCount());
        assertEquals(1, primary.getCorrectStreak());
        assertEquals(0, primary.getWrongStreak());

        assertEquals(0.0, secondary.getMasteryScore(), 0.001);
        assertEquals(1, secondary.getEvidenceCount());
        assertEquals(0, secondary.getCorrectStreak());
        assertEquals(1, secondary.getWrongStreak());

        assertTrue(masteryApplicationService.findAnswer(answerId).orElseThrow().getMasteryApplied());
        assertNotNull(primary.getLastEffectiveStudyAt());
    }

    @Test
    void upsertsMasteryAndClampsScoresToBounds() {
        Fixture fixture = fixture("mastery-clamp");

        UUID first = recordAnswer(303L, fixture.questionId(), true);
        assertTrue(masteryApplicationService.apply(
                first,
                List.of(new MasteryDelta(fixture.primaryKnowledgeId(), 95.0, true))));

        UUID second = recordAnswer(303L, fixture.questionId(), true);
        assertTrue(masteryApplicationService.apply(
                second,
                List.of(new MasteryDelta(fixture.primaryKnowledgeId(), 20.0, true))));

        UserKnowledgeMasteryEntity mastery = masteryApplicationService
                .findMastery(303L, fixture.primaryKnowledgeId()).orElseThrow();

        assertEquals(100.0, mastery.getMasteryScore(), 0.001);
        assertEquals(2, mastery.getEvidenceCount());
        assertEquals(2, mastery.getCorrectStreak());
        assertEquals(0, mastery.getWrongStreak());

        UUID third = recordAnswer(303L, fixture.questionId(), false);
        assertTrue(masteryApplicationService.apply(
                third,
                List.of(new MasteryDelta(fixture.primaryKnowledgeId(), -150.0, false))));

        mastery = masteryApplicationService.findMastery(303L, fixture.primaryKnowledgeId()).orElseThrow();
        assertEquals(0.0, mastery.getMasteryScore(), 0.001);
        assertEquals(3, mastery.getEvidenceCount());
        assertEquals(0, mastery.getCorrectStreak());
        assertEquals(1, mastery.getWrongStreak());
    }

    @Test
    void rejectsInvalidDeltasBeforeClaimingAnswer() {
        Fixture fixture = fixture("mastery-validation");
        UUID answerId = recordAnswer(404L, fixture.questionId(), true);

        assertThrows(IllegalArgumentException.class, () ->
                masteryApplicationService.apply(answerId, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                masteryApplicationService.apply(
                        answerId,
                        List.of(
                                new MasteryDelta(fixture.primaryKnowledgeId(), 5.0, true),
                                new MasteryDelta(fixture.primaryKnowledgeId(), 4.0, true))));
        assertThrows(IllegalArgumentException.class, () ->
                masteryApplicationService.apply(
                        answerId,
                        List.of(new MasteryDelta(fixture.primaryKnowledgeId(), Double.NaN, true))));

        assertFalse(masteryApplicationService.findAnswer(answerId).orElseThrow().getMasteryApplied());
    }

    private UUID recordAnswer(long userId, long questionId, boolean correct) {
        return masteryApplicationService.recordAnswer(
                userId,
                questionId,
                UUID.randomUUID(),
                correct ? "B" : "A",
                correct,
                12,
                null,
                AnswerSource.DAILY_PLAN);
    }

    private Fixture fixture(String prefix) {
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long primary = knowledgePointService.create(
                examId, null, 1, prefix + "-primary", "Primary", null,
                5, 80, 10, 0, KnowledgeStatus.ACTIVE);
        long secondary = knowledgePointService.create(
                examId, null, 1, prefix + "-secondary", "Secondary", null,
                3, 50, 10, 1, KnowledgeStatus.ACTIVE);

        long questionId = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                "Question for " + prefix,
                List.of(
                        new QuestionKnowledgeLink(primary, 0.7, true),
                        new QuestionKnowledgeLink(secondary, 0.3, false)));

        return new Fixture(questionId, primary, secondary);
    }

    private record Fixture(long questionId, long primaryKnowledgeId, long secondaryKnowledgeId) {
    }
}
