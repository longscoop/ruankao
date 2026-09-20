package com.longscoop.ruankao.assessment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.assessment.persistence.AssessmentItemEntity;
import com.longscoop.ruankao.assessment.persistence.AssessmentItemMapper;
import com.longscoop.ruankao.assessment.persistence.AssessmentSessionEntity;
import com.longscoop.ruankao.assessment.persistence.AssessmentSessionMapper;
import com.longscoop.ruankao.learning.engine.MasteryScoreCalculator;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.MasteryUpdateInput;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.learning.persistence.MasteryDelta;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeMapper;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.persistence.UserExamProfileEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssessmentService {

    private final AssessmentSessionMapper sessionMapper;
    private final AssessmentItemMapper itemMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final MasteryApplicationService masteryApplicationService;
    private final UserExamProfileService profileService;
    private final MasteryScoreCalculator masteryScoreCalculator = new MasteryScoreCalculator();

    public AssessmentService(
            AssessmentSessionMapper sessionMapper,
            AssessmentItemMapper itemMapper,
            QuestionMapper questionMapper,
            QuestionKnowledgeMapper questionKnowledgeMapper,
            MasteryApplicationService masteryApplicationService,
            UserExamProfileService profileService) {
        this.sessionMapper = sessionMapper;
        this.itemMapper = itemMapper;
        this.questionMapper = questionMapper;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.masteryApplicationService = masteryApplicationService;
        this.profileService = profileService;
    }

    @Transactional
    public UUID start(long userId, long examId, int questionCount) {
        if (userId <= 0 || examId <= 0 || questionCount <= 0) {
            throw new IllegalArgumentException("userId, examId and questionCount must be positive");
        }

        UserExamProfileEntity profile = profileService.find(userId)
                .orElseThrow(() -> new IllegalStateException("user exam profile is required"));
        if (!profile.getExamId().equals(examId)) {
            throw new IllegalArgumentException("assessment exam must match user profile");
        }

        List<QuestionEntity> questions = questionMapper.selectList(
                Wrappers.<QuestionEntity>lambdaQuery()
                        .eq(QuestionEntity::getExamId, examId)
                        .eq(QuestionEntity::getStatus, QuestionStatus.PUBLISHED)
                        .orderByAsc(QuestionEntity::getId)
                        .last("limit " + questionCount));

        if (questions.size() < questionCount) {
            throw new IllegalStateException("insufficient published questions");
        }

        AssessmentSessionEntity session = new AssessmentSessionEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setExamId(examId);
        session.setStatus(AssessmentStatus.IN_PROGRESS);
        sessionMapper.insert(session);

        for (int index = 0; index < questions.size(); index++) {
            AssessmentItemEntity item = new AssessmentItemEntity();
            item.setSessionId(session.getId());
            item.setQuestionId(questions.get(index).getId());
            item.setSortOrder(index + 1);
            itemMapper.insert(item);
        }

        return session.getId();
    }

    @Transactional(readOnly = true)
    public List<QuestionEntity> listQuestions(UUID sessionId) {
        requireSession(sessionId);
        List<AssessmentItemEntity> items = listItems(sessionId);
        List<QuestionEntity> questions = new ArrayList<>(items.size());
        for (AssessmentItemEntity item : items) {
            QuestionEntity question = questionMapper.selectById(item.getQuestionId());
            if (question == null) {
                throw new IllegalStateException("assessment question no longer exists");
            }
            questions.add(question);
        }
        return questions;
    }

    @Transactional
    public AssessmentAnswerResult answer(
            UUID sessionId,
            long questionId,
            String submittedAnswer,
            Integer durationSeconds,
            AnswerConfidence confidence) {
        AssessmentSessionEntity session = requireSession(sessionId);
        if (session.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("assessment is not in progress");
        }

        AssessmentItemEntity item = itemMapper.selectOne(
                Wrappers.<AssessmentItemEntity>lambdaQuery()
                        .eq(AssessmentItemEntity::getSessionId, sessionId)
                        .eq(AssessmentItemEntity::getQuestionId, questionId));
        if (item == null) {
            throw new IllegalArgumentException("question is not part of assessment");
        }
        if (item.getAnswerRecordId() != null) {
            throw new IllegalStateException("assessment item already answered");
        }

        QuestionEntity question = questionMapper.selectById(questionId);
        if (question == null || question.getStandardAnswer() == null) {
            throw new IllegalStateException("question standard answer is unavailable");
        }

        boolean correct = normalize(question.getStandardAnswer())
                .equals(normalize(submittedAnswer));

        UUID answerRecordId = masteryApplicationService.recordAnswer(
                session.getUserId(),
                questionId,
                sessionId,
                submittedAnswer,
                correct,
                durationSeconds,
                confidence,
                AnswerSource.ASSESSMENT);

        List<QuestionKnowledgeEntity> links = questionKnowledgeMapper.selectList(
                Wrappers.<QuestionKnowledgeEntity>lambdaQuery()
                        .eq(QuestionKnowledgeEntity::getQuestionId, questionId)
                        .orderByAsc(QuestionKnowledgeEntity::getId));

        List<MasteryDelta> deltas = new ArrayList<>(links.size());
        for (QuestionKnowledgeEntity link : links) {
            Optional<UserKnowledgeMasteryEntity> existing =
                    masteryApplicationService.findMastery(session.getUserId(), link.getKnowledgeId());
            double currentScore = existing.map(UserKnowledgeMasteryEntity::getMasteryScore).orElse(0.0);
            int previousStreak = existing
                    .map(mastery -> correct ? mastery.getCorrectStreak() : mastery.getWrongStreak())
                    .orElse(0);
            double nextScore = masteryScoreCalculator.calculate(
                    currentScore,
                    new MasteryUpdateInput(
                            correct,
                            question.getDifficulty(),
                            confidence,
                            previousStreak + 1,
                            link.getWeight()));
            deltas.add(new MasteryDelta(
                    link.getKnowledgeId(),
                    nextScore - currentScore,
                    correct));
        }

        if (!masteryApplicationService.apply(answerRecordId, deltas)) {
            throw new IllegalStateException("mastery was already applied");
        }

        if (itemMapper.attachAnswer(sessionId, questionId, answerRecordId) != 1) {
            throw new IllegalStateException("assessment item already answered");
        }

        return new AssessmentAnswerResult(questionId, correct, answerRecordId);
    }

    @Transactional
    public AssessmentResult submit(UUID sessionId) {
        AssessmentSessionEntity session = requireSession(sessionId);
        if (session.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("assessment is not in progress");
        }

        List<AssessmentItemEntity> items = listItems(sessionId);
        if (items.isEmpty() || items.stream().anyMatch(item -> item.getAnswerRecordId() == null)) {
            throw new IllegalStateException("all assessment items must be answered");
        }

        int correct = 0;
        for (AssessmentItemEntity item : items) {
            var answer = masteryApplicationService.findAnswer(item.getAnswerRecordId())
                    .orElseThrow(() -> new IllegalStateException("answer record is missing"));
            if (Boolean.TRUE.equals(answer.getCorrect())) {
                correct++;
            }
        }

        session.setStatus(AssessmentStatus.SUBMITTED);
        session.setSubmittedAt(OffsetDateTime.now());
        sessionMapper.updateById(session);
        profileService.markAssessmentCompleted(session.getUserId(), session.getExamId());

        return new AssessmentResult(items.size(), correct);
    }

    @Transactional(readOnly = true)
    public Optional<AssessmentSessionEntity> findSession(UUID sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionMapper.selectById(sessionId));
    }

    private AssessmentSessionEntity requireSession(UUID sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        AssessmentSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("assessment session not found");
        }
        return session;
    }

    private List<AssessmentItemEntity> listItems(UUID sessionId) {
        return itemMapper.selectList(
                Wrappers.<AssessmentItemEntity>lambdaQuery()
                        .eq(AssessmentItemEntity::getSessionId, sessionId)
                        .orderByAsc(AssessmentItemEntity::getSortOrder));
    }

    private String normalize(String answer) {
        return answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
    }
}
