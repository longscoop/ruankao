package com.longscoop.ruankao.learning;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.engine.DailyPlanAllocator;
import com.longscoop.ruankao.learning.engine.PriorityScoreCalculator;
import com.longscoop.ruankao.learning.engine.RetentionCalculator;
import com.longscoop.ruankao.learning.model.DailyPlanAllocation;
import com.longscoop.ruankao.learning.model.PriorityInput;
import com.longscoop.ruankao.learning.model.StudyPlanStatus;
import com.longscoop.ruankao.learning.model.StudyTaskStatus;
import com.longscoop.ruankao.learning.model.StudyTaskType;
import com.longscoop.ruankao.learning.persistence.AnswerRecordMapper;
import com.longscoop.ruankao.learning.persistence.StudyPlanEntity;
import com.longscoop.ruankao.learning.persistence.StudyPlanMapper;
import com.longscoop.ruankao.learning.persistence.StudyTaskEntity;
import com.longscoop.ruankao.learning.persistence.StudyTaskMapper;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryMapper;
import com.longscoop.ruankao.question.model.WrongQuestionStatus;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import com.longscoop.ruankao.question.persistence.WrongQuestionMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.persistence.UserExamProfileEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DailyLearningService {

    private final UserExamProfileService profileService;
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserKnowledgeMasteryMapper masteryMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final StudyTaskMapper studyTaskMapper;

    private final DailyPlanAllocator allocator = new DailyPlanAllocator();
    private final RetentionCalculator retentionCalculator = new RetentionCalculator();
    private final PriorityScoreCalculator priorityCalculator = new PriorityScoreCalculator();

    public DailyLearningService(
            UserExamProfileService profileService,
            KnowledgePointMapper knowledgePointMapper,
            UserKnowledgeMasteryMapper masteryMapper,
            WrongQuestionMapper wrongQuestionMapper,
            AnswerRecordMapper answerRecordMapper,
            StudyPlanMapper studyPlanMapper,
            StudyTaskMapper studyTaskMapper) {
        this.profileService = profileService;
        this.knowledgePointMapper = knowledgePointMapper;
        this.masteryMapper = masteryMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.studyPlanMapper = studyPlanMapper;
        this.studyTaskMapper = studyTaskMapper;
    }

    @Transactional
    public DailyPlanView getOrCreateToday(long userId, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }

        UserExamProfileEntity profile = profileService.find(userId)
                .orElseThrow(() -> new IllegalArgumentException("user exam profile not found"));

        LocalDate planDate = LocalDate.now(clock);
        StudyPlanEntity existing = findPlan(userId, profile.getExamId(), planDate);
        if (existing != null) {
            return toView(existing);
        }

        long daysUntilExam = ChronoUnit.DAYS.between(planDate, profile.getExamDate());
        if (daysUntilExam < 0) {
            throw new IllegalStateException("exam date is before plan date");
        }

        boolean hasActiveWrong = wrongQuestionMapper.selectCount(
                Wrappers.<WrongQuestionEntity>lambdaQuery()
                        .eq(WrongQuestionEntity::getUserId, userId)
                        .eq(WrongQuestionEntity::getStatus, WrongQuestionStatus.ACTIVE)) > 0;

        DailyPlanAllocation base = allocator.allocate(
                profile.getDailyTargetMinutes(),
                Math.toIntExact(daysUntilExam),
                hasActiveWrong);

        List<KnowledgePointEntity> knowledgePoints = knowledgePointMapper.selectList(
                Wrappers.<KnowledgePointEntity>lambdaQuery()
                        .eq(KnowledgePointEntity::getExamId, profile.getExamId())
                        .orderByAsc(KnowledgePointEntity::getSortOrder)
                        .orderByAsc(KnowledgePointEntity::getId));

        List<UserKnowledgeMasteryEntity> masteryRows = masteryMapper.selectList(
                Wrappers.<UserKnowledgeMasteryEntity>lambdaQuery()
                        .eq(UserKnowledgeMasteryEntity::getUserId, userId));

        Candidate weakCandidate = selectWeakCandidate(userId, planDate, clock, knowledgePoints, masteryRows);
        KnowledgePointEntity newCandidate = selectNewCandidate(knowledgePoints, masteryRows);

        int wrongMinutes = base.wrongReviewMinutes();
        int weakMinutes = base.weakPointMinutes();
        int newMinutes = base.newKnowledgeMinutes();
        int realExamMinutes = base.realExamMinutes();

        if (weakMinutes > 0 && weakCandidate == null) {
            newMinutes += weakMinutes;
            weakMinutes = 0;
        }
        if (newMinutes > 0 && newCandidate == null) {
            if (weakCandidate != null) {
                weakMinutes += newMinutes;
            } else {
                realExamMinutes += newMinutes;
            }
            newMinutes = 0;
        }

        int estimatedMinutes = wrongMinutes + weakMinutes + newMinutes + realExamMinutes;
        studyPlanMapper.insertIfAbsent(
                userId,
                profile.getExamId(),
                planDate,
                profile.getDailyTargetMinutes(),
                estimatedMinutes);

        StudyPlanEntity plan = findPlan(userId, profile.getExamId(), planDate);
        if (plan == null) {
            throw new IllegalStateException("study plan was not persisted");
        }

        if (studyTaskMapper.selectCount(
                Wrappers.<StudyTaskEntity>lambdaQuery()
                        .eq(StudyTaskEntity::getPlanId, plan.getId())) == 0) {
            int sortOrder = 1;
            if (wrongMinutes > 0) {
                insertTask(plan.getId(), StudyTaskType.WRONG_REVIEW, null, wrongMinutes, null, sortOrder++);
            }
            if (weakMinutes > 0 && weakCandidate != null) {
                insertTask(
                        plan.getId(),
                        StudyTaskType.WEAK_POINT,
                        weakCandidate.knowledge().getId(),
                        weakMinutes,
                        weakCandidate.priorityScore(),
                        sortOrder++);
            }
            if (newMinutes > 0 && newCandidate != null) {
                insertTask(
                        plan.getId(),
                        StudyTaskType.NEW_KNOWLEDGE,
                        newCandidate.getId(),
                        newMinutes,
                        null,
                        sortOrder++);
            }
            if (realExamMinutes > 0) {
                insertTask(plan.getId(), StudyTaskType.REAL_EXAM, null, realExamMinutes, null, sortOrder);
            }
        }

        return toView(plan);
    }

    @Transactional(readOnly = true)
    public long countPlans(long userId, long examId, LocalDate planDate) {
        return studyPlanMapper.selectCount(
                Wrappers.<StudyPlanEntity>lambdaQuery()
                        .eq(StudyPlanEntity::getUserId, userId)
                        .eq(StudyPlanEntity::getExamId, examId)
                        .eq(StudyPlanEntity::getPlanDate, planDate));
    }

    private Candidate selectWeakCandidate(
            long userId,
            LocalDate planDate,
            Clock clock,
            List<KnowledgePointEntity> knowledgePoints,
            List<UserKnowledgeMasteryEntity> masteryRows) {
        return masteryRows.stream()
                .filter(row -> row.getEvidenceCount() != null && row.getEvidenceCount() > 0)
                .map(row -> {
                    KnowledgePointEntity knowledge = knowledgePoints.stream()
                            .filter(point -> point.getId().equals(row.getKnowledgeId()))
                            .findFirst()
                            .orElse(null);
                    if (knowledge == null) {
                        return null;
                    }

                    long daysSinceStudy = daysSince(row.getLastEffectiveStudyAt(), planDate, clock);
                    double retention = retentionCalculator.retentionFactor(daysSinceStudy);
                    double effectiveMastery = retentionCalculator.effectiveMastery(
                            row.getMasteryScore(),
                            daysSinceStudy);
                    int recentWrongCount = countRecentWrongAnswers(
                            userId,
                            knowledge.getId(),
                            OffsetDateTime.now(clock).minusDays(14));
                    double priority = priorityCalculator.calculate(
                            new PriorityInput(
                                    effectiveMastery,
                                    knowledge.getImportance(),
                                    retention,
                                    knowledge.getExamFrequency(),
                                    recentWrongCount));
                    return new Candidate(knowledge, priority);
                })
                .filter(candidate -> candidate != null)
                .max(Comparator.comparingDouble(Candidate::priorityScore))
                .orElse(null);
    }

    private KnowledgePointEntity selectNewCandidate(
            List<KnowledgePointEntity> knowledgePoints,
            List<UserKnowledgeMasteryEntity> masteryRows) {
        return knowledgePoints.stream()
                .filter(point -> masteryRows.stream()
                        .noneMatch(row -> row.getKnowledgeId().equals(point.getId())
                                && row.getEvidenceCount() != null
                                && row.getEvidenceCount() > 0))
                .findFirst()
                .orElse(null);
    }

    private int countRecentWrongAnswers(long userId, long knowledgeId, OffsetDateTime since) {
        Integer count = answerRecordMapper.countRecentWrongByKnowledge(userId, knowledgeId, since);
        return count == null ? 0 : count;
    }

    private long daysSince(OffsetDateTime lastStudy, LocalDate planDate, Clock clock) {
        if (lastStudy == null) {
            return 0;
        }
        LocalDate lastDate = lastStudy.atZoneSameInstant(clock.getZone()).toLocalDate();
        return Math.max(0, ChronoUnit.DAYS.between(lastDate, planDate));
    }

    private StudyPlanEntity findPlan(long userId, long examId, LocalDate planDate) {
        return studyPlanMapper.selectOne(
                Wrappers.<StudyPlanEntity>lambdaQuery()
                        .eq(StudyPlanEntity::getUserId, userId)
                        .eq(StudyPlanEntity::getExamId, examId)
                        .eq(StudyPlanEntity::getPlanDate, planDate));
    }

    private void insertTask(
            long planId,
            StudyTaskType type,
            Long knowledgeId,
            int estimatedMinutes,
            Double priorityScore,
            int sortOrder) {
        StudyTaskEntity task = new StudyTaskEntity();
        task.setPlanId(planId);
        task.setTaskType(type);
        task.setKnowledgeId(knowledgeId);
        task.setTargetQuestionCount(0);
        task.setCompletedQuestionCount(0);
        task.setEstimatedMinutes(estimatedMinutes);
        task.setActualMinutes(0);
        task.setPriorityScore(priorityScore);
        task.setSortOrder(sortOrder);
        task.setStatus(StudyTaskStatus.PENDING);
        studyTaskMapper.insert(task);
    }

    private DailyPlanView toView(StudyPlanEntity plan) {
        List<StudyTaskEntity> tasks = studyTaskMapper.selectList(
                Wrappers.<StudyTaskEntity>lambdaQuery()
                        .eq(StudyTaskEntity::getPlanId, plan.getId())
                        .orderByAsc(StudyTaskEntity::getSortOrder));

        List<StudyTaskView> views = new ArrayList<>(tasks.size());
        for (StudyTaskEntity task : tasks) {
            views.add(new StudyTaskView(
                    task.getId(),
                    task.getTaskType(),
                    task.getKnowledgeId(),
                    task.getEstimatedMinutes(),
                    task.getPriorityScore()));
        }

        return new DailyPlanView(
                plan.getId(),
                plan.getPlanDate(),
                plan.getTargetMinutes(),
                List.copyOf(views));
    }

    private record Candidate(KnowledgePointEntity knowledge, double priorityScore) {
    }
}
