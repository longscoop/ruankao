package com.longscoop.ruankao.learning;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.learning.model.StudyTaskType;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.learning.persistence.MasteryDelta;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.WrongQuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class DailyLearningServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC);

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
    private WrongQuestionService wrongQuestionService;

    @Autowired
    private DailyLearningService dailyLearningService;

    @Test
    void createsNormalDailyPlanFromLearningSignals() {
        Fixture fixture = fixture("daily-normal", LocalDate.of(2026, 10, 20));
        seedEvidence(fixture.userId(), fixture.weakQuestionId(), fixture.weakKnowledgeId(), 40.0, true);
        wrongQuestionService.recordWrong(fixture.userId(), fixture.weakQuestionId());

        DailyPlanView plan = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);

        assertEquals(LocalDate.of(2026, 9, 20), plan.planDate());
        assertEquals(30, plan.targetMinutes());
        assertEquals(30, plan.tasks().stream().mapToInt(StudyTaskView::estimatedMinutes).sum());
        assertTask(plan, StudyTaskType.WRONG_REVIEW, null, 6);
        assertTask(plan, StudyTaskType.WEAK_POINT, fixture.weakKnowledgeId(), 11);
        assertTask(plan, StudyTaskType.NEW_KNOWLEDGE, fixture.newKnowledgeId(), 7);
        assertTask(plan, StudyTaskType.REAL_EXAM, null, 6);
        assertNotNull(task(plan, StudyTaskType.WEAK_POINT).priorityScore());
    }

    @Test
    void repeatedRequestOnSameDateReturnsSamePlanWithoutDuplicateTasks() {
        Fixture fixture = fixture("daily-idempotent", LocalDate.of(2026, 10, 20));
        seedEvidence(fixture.userId(), fixture.weakQuestionId(), fixture.weakKnowledgeId(), 50.0, true);

        DailyPlanView first = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);
        DailyPlanView second = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);

        assertEquals(first.planId(), second.planId());
        assertEquals(first.tasks().size(), second.tasks().size());
        assertEquals(1L, dailyLearningService.countPlans(
                fixture.userId(), fixture.examId(), LocalDate.of(2026, 9, 20)));
    }

    @Test
    void movesWrongReviewAllocationToWeakPointWhenNoWrongQuestionIsActive() {
        Fixture fixture = fixture("daily-no-wrong", LocalDate.of(2026, 10, 20));
        seedEvidence(fixture.userId(), fixture.weakQuestionId(), fixture.weakKnowledgeId(), 45.0, true);

        DailyPlanView plan = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);

        assertEquals(3, plan.tasks().size());
        assertTask(plan, StudyTaskType.WEAK_POINT, fixture.weakKnowledgeId(), 17);
        assertTask(plan, StudyTaskType.NEW_KNOWLEDGE, fixture.newKnowledgeId(), 7);
        assertTask(plan, StudyTaskType.REAL_EXAM, null, 6);
    }

    @Test
    void usesSprintAllocationWithinFourteenDays() {
        Fixture fixture = fixture("daily-sprint", LocalDate.of(2026, 10, 4));
        seedEvidence(fixture.userId(), fixture.weakQuestionId(), fixture.weakKnowledgeId(), 40.0, false);
        wrongQuestionService.recordWrong(fixture.userId(), fixture.weakQuestionId());

        DailyPlanView plan = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);

        assertTask(plan, StudyTaskType.WRONG_REVIEW, null, 9);
        assertTask(plan, StudyTaskType.WEAK_POINT, fixture.weakKnowledgeId(), 11);
        assertTask(plan, StudyTaskType.NEW_KNOWLEDGE, fixture.newKnowledgeId(), 3);
        assertTask(plan, StudyTaskType.REAL_EXAM, null, 7);
    }

    @Test
    void redistributesUnavailableWeakAllocationWithoutLosingMinutes() {
        Fixture fixture = fixture("daily-no-weak", LocalDate.of(2026, 10, 20));

        DailyPlanView plan = dailyLearningService.getOrCreateToday(fixture.userId(), CLOCK);

        assertEquals(30, plan.tasks().stream().mapToInt(StudyTaskView::estimatedMinutes).sum());
        assertEquals(2, plan.tasks().size());
        assertTask(plan, StudyTaskType.NEW_KNOWLEDGE, fixture.weakKnowledgeId(), 24);
        assertTask(plan, StudyTaskType.REAL_EXAM, null, 6);
    }

    private Fixture fixture(String prefix, LocalDate examDate) {
        long userId = Math.abs((long) prefix.hashCode()) + 100_000L;
        long examId = examService.create(prefix + "-exam", prefix, ExamStatus.ACTIVE);
        long weakKnowledge = knowledgePointService.create(
                examId, null, 1, prefix + "-weak", "Weak", null,
                5, 90, 10, 1, KnowledgeStatus.ACTIVE);
        long newKnowledge = knowledgePointService.create(
                examId, null, 1, prefix + "-new", "New", null,
                3, 50, 10, 2, KnowledgeStatus.ACTIVE);
        long weakQuestion = questionService.create(
                examId,
                QuestionType.SINGLE_CHOICE,
                QuestionSource.REAL_EXAM,
                QuestionStatus.PUBLISHED,
                QuestionDifficulty.MEDIUM,
                "Weak question",
                "A",
                List.of(new QuestionKnowledgeLink(weakKnowledge, 1.0, true)));

        profileService.upsert(userId, examId, examDate, 30, FoundationLevel.SOME);
        return new Fixture(userId, examId, weakKnowledge, newKnowledge, weakQuestion);
    }

    private void seedEvidence(
            long userId,
            long questionId,
            long knowledgeId,
            double delta,
            boolean correct) {
        UUID answerId = masteryApplicationService.recordAnswer(
                userId,
                questionId,
                UUID.randomUUID(),
                correct ? "A" : "B",
                correct,
                10,
                null,
                AnswerSource.DAILY_PLAN);
        masteryApplicationService.apply(
                answerId,
                List.of(new MasteryDelta(knowledgeId, delta, correct)));
    }

    private void assertTask(
            DailyPlanView plan,
            StudyTaskType type,
            Long knowledgeId,
            int minutes) {
        StudyTaskView task = task(plan, type);
        assertEquals(knowledgeId, task.knowledgeId());
        assertEquals(minutes, task.estimatedMinutes());
    }

    private StudyTaskView task(DailyPlanView plan, StudyTaskType type) {
        return plan.tasks().stream()
                .filter(task -> task.taskType() == type)
                .findFirst()
                .orElseThrow();
    }

    private record Fixture(
            long userId,
            long examId,
            long weakKnowledgeId,
            long newKnowledgeId,
            long weakQuestionId) {
    }
}
