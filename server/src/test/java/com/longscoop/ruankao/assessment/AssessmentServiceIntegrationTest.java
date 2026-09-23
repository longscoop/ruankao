package com.longscoop.ruankao.assessment;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AssessmentServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserExamProfileService profileService;

    @Autowired
    private MasteryApplicationService masteryApplicationService;

    @Autowired
    private AssessmentService assessmentService;

    @Test
    void selectsPublishedQuestionsDeterministicallyById() {
        Fixture fixture = fixture("assessment-order");
        long first = publishedQuestion(fixture, "Q1", "A");
        long second = publishedQuestion(fixture, "Q2", "B");
        publishedQuestion(fixture, "Q3", "C");
        draftQuestion(fixture, "Draft", "D");

        UUID sessionId = assessmentService.start(fixture.userId(), fixture.examId(), 2);

        List<QuestionEntity> questions = assessmentService.listQuestions(sessionId);
        assertEquals(List.of(first, second), questions.stream().map(QuestionEntity::getId).toList());
    }

    @Test
    void startFailsWhenPublishedQuestionsAreInsufficient() {
        Fixture fixture = fixture("assessment-short");
        publishedQuestion(fixture, "Only one", "A");

        assertThrows(IllegalStateException.class, () ->
                assessmentService.start(fixture.userId(), fixture.examId(), 2));
    }

    @Test
    void answerUsesPersistedStandardAnswerAndUpdatesMastery() {
        Fixture fixture = fixture("assessment-answer");
        long questionId = publishedQuestion(fixture, "Correct answer is B", "B");
        UUID sessionId = assessmentService.start(fixture.userId(), fixture.examId(), 1);

        AssessmentAnswerResult result = assessmentService.answer(
                sessionId,
                questionId,
                " B ",
                20,
                AnswerConfidence.CONFIDENT);

        assertTrue(result.correct());
        assertEquals(questionId, result.questionId());

        var mastery = masteryApplicationService
                .findMastery(fixture.userId(), fixture.knowledgeId())
                .orElseThrow();
        assertEquals(5.0, mastery.getMasteryScore(), 0.001);
        assertEquals(1, mastery.getEvidenceCount());
        assertEquals(1, mastery.getCorrectStreak());
    }

    @Test
    void assessmentItemCanOnlyBeAnsweredOnce() {
        Fixture fixture = fixture("assessment-once");
        long questionId = publishedQuestion(fixture, "One answer", "A");
        UUID sessionId = assessmentService.start(fixture.userId(), fixture.examId(), 1);

        assessmentService.answer(sessionId, questionId, "A", 5, null);

        assertThrows(IllegalStateException.class, () ->
                assessmentService.answer(sessionId, questionId, "A", 5, null));
    }

    @Test
    void submitRequiresAllItemsAndMarksProfileAssessmentCompleted() {
        Fixture fixture = fixture("assessment-submit");
        long first = publishedQuestion(fixture, "First", "A");
        long second = publishedQuestion(fixture, "Second", "B");
        UUID sessionId = assessmentService.start(fixture.userId(), fixture.examId(), 2);

        assessmentService.answer(sessionId, first, "A", 5, AnswerConfidence.UNCERTAIN);
        assertThrows(IllegalStateException.class, () -> assessmentService.submit(sessionId));
        assertFalse(profileService.find(fixture.userId()).orElseThrow().getAssessmentCompleted());

        assessmentService.answer(sessionId, second, "C", 5, AnswerConfidence.UNCERTAIN);
        AssessmentResult result = assessmentService.submit(sessionId);

        assertEquals(2, result.totalQuestions());
        assertEquals(1, result.correctQuestions());
        assertTrue(profileService.find(fixture.userId()).orElseThrow().getAssessmentCompleted());
        assertEquals(AssessmentStatus.SUBMITTED, assessmentService.findSession(sessionId).orElseThrow().getStatus());
    }

    private Fixture fixture(String prefix) {
        long userId = Math.abs((long) prefix.hashCode()) + 10_000L;
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId,
                null,
                1,
                prefix + "-knowledge",
                "Knowledge",
                null,
                5,
                80,
                10,
                0,
                KnowledgeStatus.ACTIVE);

        profileService.upsert(
                userId,
                examId,
                LocalDate.now().plusDays(60),
                30,
                FoundationLevel.SOME);

        return new Fixture(userId, examId, knowledgeId);
    }

    private long publishedQuestion(Fixture fixture, String content, String answer) {
        return questionService.create(
                fixture.examId(),
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                content,
                answer,
                List.of(new QuestionKnowledgeLink(fixture.knowledgeId(), 1.0, true)));
    }

    private long draftQuestion(Fixture fixture, String content, String answer) {
        return questionService.create(
                fixture.examId(),
                QuestionType.SINGLE_CHOICE,
                QuestionSource.MANUAL,
                QuestionStatus.DRAFT,
                QuestionDifficulty.MEDIUM,
                content,
                answer,
                List.of(new QuestionKnowledgeLink(fixture.knowledgeId(), 1.0, true)));
    }

    private record Fixture(long userId, long examId, long knowledgeId) {
    }
}
