package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class MasteryApplicationService {

    private final AnswerRecordMapper answerRecordMapper;
    private final UserKnowledgeMasteryMapper userKnowledgeMasteryMapper;

    public MasteryApplicationService(
            AnswerRecordMapper answerRecordMapper,
            UserKnowledgeMasteryMapper userKnowledgeMasteryMapper) {
        this.answerRecordMapper = answerRecordMapper;
        this.userKnowledgeMasteryMapper = userKnowledgeMasteryMapper;
    }

    @Transactional
    public UUID recordAnswer(
            long userId,
            long questionId,
            UUID sessionId,
            String answer,
            boolean correct,
            Integer durationSeconds,
            AnswerConfidence confidence,
            AnswerSource source) {
        if (userId <= 0 || questionId <= 0) {
            throw new IllegalArgumentException("userId and questionId must be positive");
        }
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        AnswerRecordEntity entity = new AnswerRecordEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setQuestionId(questionId);
        entity.setSessionId(sessionId);
        entity.setAnswer(answer);
        entity.setCorrect(correct);
        entity.setDurationSeconds(durationSeconds);
        entity.setConfidence(confidence);
        entity.setSource(source);
        entity.setMasteryApplied(false);

        answerRecordMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public IdempotentAnswerRecord recordAnswerIdempotent(
            long userId,
            long questionId,
            UUID sessionId,
            String idempotencyKey,
            String answer,
            boolean correct,
            Integer durationSeconds,
            AnswerConfidence confidence,
            AnswerSource source) {
        if (userId <= 0 || questionId <= 0) {
            throw new IllegalArgumentException("userId and questionId must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (idempotencyKey.trim().length() > 128) {
            throw new IllegalArgumentException("idempotencyKey is too long");
        }
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        String normalizedKey = idempotencyKey.trim();
        AnswerRecordEntity existing =
                answerRecordMapper.selectByIdempotencyKey(userId, normalizedKey);
        if (existing != null) {
            return new IdempotentAnswerRecord(existing, false);
        }

        AnswerRecordEntity entity = new AnswerRecordEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setQuestionId(questionId);
        entity.setSessionId(sessionId);
        entity.setAnswer(answer);
        entity.setCorrect(correct);
        entity.setDurationSeconds(durationSeconds);
        entity.setConfidence(confidence);
        entity.setSource(source);
        entity.setIdempotencyKey(normalizedKey);
        entity.setMasteryApplied(false);

        boolean created = answerRecordMapper.insertIdempotent(entity) == 1;
        AnswerRecordEntity persisted =
                answerRecordMapper.selectByIdempotencyKey(userId, normalizedKey);
        if (persisted == null) {
            throw new IllegalStateException("idempotent answer was not persisted");
        }
        return new IdempotentAnswerRecord(persisted, created);
    }

    @Transactional(readOnly = true)
    public Optional<AnswerRecordEntity> findByIdempotencyKey(long userId, String idempotencyKey) {
        if (userId <= 0 || idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                answerRecordMapper.selectByIdempotencyKey(userId, idempotencyKey.trim()));
    }

    @Transactional(readOnly = true)
    public Optional<AnswerRecordEntity> findAnswer(UUID answerId) {
        if (answerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(answerRecordMapper.selectById(answerId));
    }

    @Transactional(readOnly = true)
    public Optional<UserKnowledgeMasteryEntity> findMastery(long userId, long knowledgeId) {
        return Optional.ofNullable(
                userKnowledgeMasteryMapper.selectOne(
                        Wrappers.<UserKnowledgeMasteryEntity>lambdaQuery()
                                .eq(UserKnowledgeMasteryEntity::getUserId, userId)
                                .eq(UserKnowledgeMasteryEntity::getKnowledgeId, knowledgeId)));
    }

    @Transactional
    public boolean apply(UUID answerId, List<MasteryDelta> deltas) {
        validateDeltas(answerId, deltas);

        AnswerRecordEntity answer = answerRecordMapper.selectById(answerId);
        if (answer == null) {
            return false;
        }

        if (answerRecordMapper.claimMastery(answerId) != 1) {
            return false;
        }

        for (MasteryDelta delta : deltas) {
            userKnowledgeMasteryMapper.applyDelta(
                    answer.getUserId(),
                    delta.knowledgeId(),
                    delta.scoreDelta(),
                    delta.correct());
        }

        return true;
    }

    private void validateDeltas(UUID answerId, List<MasteryDelta> deltas) {
        if (answerId == null) {
            throw new IllegalArgumentException("answerId is required");
        }
        if (deltas == null || deltas.isEmpty()) {
            throw new IllegalArgumentException("at least one mastery delta is required");
        }

        Set<Long> knowledgeIds = new HashSet<>();
        for (MasteryDelta delta : deltas) {
            if (delta == null || delta.knowledgeId() <= 0) {
                throw new IllegalArgumentException("valid mastery delta is required");
            }
            if (!Double.isFinite(delta.scoreDelta())) {
                throw new IllegalArgumentException("scoreDelta must be finite");
            }
            if (!knowledgeIds.add(delta.knowledgeId())) {
                throw new IllegalArgumentException("duplicate mastery delta knowledgeId");
            }
        }
    }
}
