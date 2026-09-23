package com.longscoop.ruankao.analytics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.course.persistence.UserVideoProgressMapper;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.persistence.AnswerRecordMapper;
import com.longscoop.ruankao.learning.persistence.StudyTaskMapper;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeeklyStatsService {

    private final UserExamProfileService profileService;
    private final AnswerRecordMapper answerRecordMapper;
    private final UserVideoProgressMapper videoProgressMapper;
    private final StudyTaskMapper studyTaskMapper;
    private final UserKnowledgeMasteryMapper masteryMapper;
    private final KnowledgePointMapper knowledgeMapper;

    public WeeklyStatsService(
            UserExamProfileService profileService,
            AnswerRecordMapper answerRecordMapper,
            UserVideoProgressMapper videoProgressMapper,
            StudyTaskMapper studyTaskMapper,
            UserKnowledgeMasteryMapper masteryMapper,
            KnowledgePointMapper knowledgeMapper) {
        this.profileService = profileService;
        this.answerRecordMapper = answerRecordMapper;
        this.videoProgressMapper = videoProgressMapper;
        this.studyTaskMapper = studyTaskMapper;
        this.masteryMapper = masteryMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    @Transactional(readOnly = true)
    public WeeklyStats get(long userId) {
        long examId = profileService.find(userId)
                .orElseThrow(() -> new IllegalStateException("exam profile is required"))
                .getExamId();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        OffsetDateTime since = weekStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime until = weekStart.plusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC);

        long answered = answerRecordMapper.countAnsweredBetween(userId, since, until);
        long correct = answerRecordMapper.countCorrectBetween(userId, since, until);
        long answerSeconds = answerRecordMapper.sumDurationSecondsBetween(userId, since, until);
        long videoSeconds = videoProgressMapper.sumProgressSecondsBetween(userId, since, until);
        long completedTasks = studyTaskMapper.countCompletedBetween(userId, weekStart, weekEnd);
        int studyMinutes = (int) Math.max(0, (answerSeconds + videoSeconds) / 60);

        List<WeakKnowledge> weakKnowledge = new ArrayList<>();
        List<UserKnowledgeMasteryEntity> masteries = masteryMapper.selectList(
                Wrappers.<UserKnowledgeMasteryEntity>lambdaQuery()
                        .eq(UserKnowledgeMasteryEntity::getUserId, userId)
                        .gt(UserKnowledgeMasteryEntity::getEvidenceCount, 0)
                        .orderByAsc(UserKnowledgeMasteryEntity::getMasteryScore)
                        .orderByAsc(UserKnowledgeMasteryEntity::getKnowledgeId));
        for (UserKnowledgeMasteryEntity mastery : masteries) {
            KnowledgePointEntity knowledge = knowledgeMapper.selectById(mastery.getKnowledgeId());
            if (knowledge == null
                    || !knowledge.getExamId().equals(examId)
                    || knowledge.getStatus() != KnowledgeStatus.ACTIVE) {
                continue;
            }
            weakKnowledge.add(new WeakKnowledge(
                    knowledge.getId(),
                    knowledge.getName(),
                    mastery.getMasteryScore(),
                    mastery.getEvidenceCount()));
            if (weakKnowledge.size() == 5) {
                break;
            }
        }

        return new WeeklyStats(
                weekStart,
                weekEnd,
                studyMinutes,
                Math.toIntExact(completedTasks),
                Math.toIntExact(answered),
                Math.toIntExact(correct),
                weakKnowledge);
    }

    public record WeeklyStats(
            LocalDate weekStart,
            LocalDate weekEnd,
            int studyMinutes,
            int completedTasks,
            int answeredQuestions,
            int correctQuestions,
            List<WeakKnowledge> weakKnowledge) {
    }

    public record WeakKnowledge(
            long knowledgeId,
            String name,
            Double masteryScore,
            int evidenceCount) {
    }
}
