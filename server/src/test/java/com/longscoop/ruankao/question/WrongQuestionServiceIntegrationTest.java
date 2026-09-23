package com.longscoop.ruankao.question;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.model.WrongQuestionStatus;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class WrongQuestionServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Test
    void firstAndRepeatedWrongAnswersKeepSingleActiveHistoryRow() {
        long questionId = createQuestion("wrong-repeat");

        wrongQuestionService.recordWrong(501L, questionId);
        WrongQuestionEntity first = wrongQuestionService.find(501L, questionId).orElseThrow();

        wrongQuestionService.recordWrong(501L, questionId);
        WrongQuestionEntity repeated = wrongQuestionService.find(501L, questionId).orElseThrow();

        assertEquals(first.getId(), repeated.getId());
        assertEquals(WrongQuestionStatus.ACTIVE, repeated.getStatus());
        assertEquals(2, repeated.getWrongCount());
        assertEquals(0, repeated.getConsecutiveCorrect());
        assertNotNull(repeated.getFirstWrongAt());
        assertNotNull(repeated.getLastWrongAt());
        assertNull(repeated.getMasteredAt());
    }

    @Test
    void mastersOnlyAfterTwoCorrectReviewsAndSufficientMastery() {
        long questionId = createQuestion("wrong-master");
        wrongQuestionService.recordWrong(502L, questionId);

        wrongQuestionService.recordCorrectReview(502L, questionId, 80.0);
        WrongQuestionEntity afterOne = wrongQuestionService.find(502L, questionId).orElseThrow();
        assertEquals(WrongQuestionStatus.ACTIVE, afterOne.getStatus());
        assertEquals(1, afterOne.getConsecutiveCorrect());

        wrongQuestionService.recordCorrectReview(502L, questionId, 69.99);
        WrongQuestionEntity belowThreshold = wrongQuestionService.find(502L, questionId).orElseThrow();
        assertEquals(WrongQuestionStatus.ACTIVE, belowThreshold.getStatus());
        assertEquals(2, belowThreshold.getConsecutiveCorrect());
        assertNull(belowThreshold.getMasteredAt());

        wrongQuestionService.recordCorrectReview(502L, questionId, 70.0);
        WrongQuestionEntity mastered = wrongQuestionService.find(502L, questionId).orElseThrow();
        assertEquals(WrongQuestionStatus.MASTERED, mastered.getStatus());
        assertEquals(3, mastered.getConsecutiveCorrect());
        assertNotNull(mastered.getMasteredAt());
    }

    @Test
    void wrongAnswerAfterMasteryReactivatesSameHistoryRow() {
        long questionId = createQuestion("wrong-reactivate");
        wrongQuestionService.recordWrong(503L, questionId);
        long originalId = wrongQuestionService.find(503L, questionId).orElseThrow().getId();

        wrongQuestionService.recordCorrectReview(503L, questionId, 90.0);
        wrongQuestionService.recordCorrectReview(503L, questionId, 90.0);
        assertEquals(
                WrongQuestionStatus.MASTERED,
                wrongQuestionService.find(503L, questionId).orElseThrow().getStatus());

        wrongQuestionService.recordWrong(503L, questionId);
        WrongQuestionEntity reactivated = wrongQuestionService.find(503L, questionId).orElseThrow();

        assertEquals(originalId, reactivated.getId());
        assertEquals(WrongQuestionStatus.ACTIVE, reactivated.getStatus());
        assertEquals(2, reactivated.getWrongCount());
        assertEquals(0, reactivated.getConsecutiveCorrect());
        assertNull(reactivated.getMasteredAt());
    }

    @Test
    void correctReviewDoesNothingWhenQuestionWasNeverWrong() {
        long questionId = createQuestion("wrong-missing");

        wrongQuestionService.recordCorrectReview(504L, questionId, 90.0);

        assertFalse(wrongQuestionService.find(504L, questionId).isPresent());
    }

    @Test
    void rejectsInvalidEffectiveMastery() {
        long questionId = createQuestion("wrong-validation");
        wrongQuestionService.recordWrong(505L, questionId);

        assertThrows(IllegalArgumentException.class, () ->
                wrongQuestionService.recordCorrectReview(505L, questionId, -0.01));
        assertThrows(IllegalArgumentException.class, () ->
                wrongQuestionService.recordCorrectReview(505L, questionId, 100.01));
        assertThrows(IllegalArgumentException.class, () ->
                wrongQuestionService.recordCorrectReview(505L, questionId, Double.NaN));
    }

    private long createQuestion(String prefix) {
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, prefix + "-kp", "Knowledge", null,
                3, 50, 10, 0, KnowledgeStatus.ACTIVE);

        return questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                "Question for " + prefix,
                List.of(new QuestionKnowledgeLink(knowledgeId, 1.0, true)));
    }
}
