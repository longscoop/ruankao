package com.longscoop.ruankao.question;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.question.model.WrongQuestionStatus;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import com.longscoop.ruankao.question.persistence.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    @Transactional
    public void recordWrong(long userId, long questionId) {
        validateIds(userId, questionId);

        WrongQuestionEntity entity = select(userId, questionId);
        OffsetDateTime now = OffsetDateTime.now();

        if (entity == null) {
            entity = new WrongQuestionEntity();
            entity.setUserId(userId);
            entity.setQuestionId(questionId);
            entity.setStatus(WrongQuestionStatus.ACTIVE);
            entity.setWrongCount(1);
            entity.setConsecutiveCorrect(0);
            entity.setFirstWrongAt(now);
            entity.setLastWrongAt(now);
            entity.setMasteredAt(null);
            entity.setUpdatedAt(now);
            wrongQuestionMapper.insert(entity);
            return;
        }

        entity.setStatus(WrongQuestionStatus.ACTIVE);
        entity.setWrongCount(entity.getWrongCount() + 1);
        entity.setConsecutiveCorrect(0);
        entity.setLastWrongAt(now);
        entity.setMasteredAt(null);
        entity.setUpdatedAt(now);
        wrongQuestionMapper.updateById(entity);
    }

    @Transactional
    public void recordCorrectReview(long userId, long questionId, double effectiveMastery) {
        validateIds(userId, questionId);
        if (!Double.isFinite(effectiveMastery)
                || effectiveMastery < 0.0
                || effectiveMastery > 100.0) {
            throw new IllegalArgumentException("effectiveMastery must be between 0 and 100");
        }

        WrongQuestionEntity entity = select(userId, questionId);
        if (entity == null) {
            return;
        }

        int consecutiveCorrect = entity.getConsecutiveCorrect() + 1;
        OffsetDateTime now = OffsetDateTime.now();

        entity.setConsecutiveCorrect(consecutiveCorrect);
        if (consecutiveCorrect >= 2 && effectiveMastery >= 70.0) {
            entity.setStatus(WrongQuestionStatus.MASTERED);
            if (entity.getMasteredAt() == null) {
                entity.setMasteredAt(now);
            }
        } else {
            entity.setStatus(WrongQuestionStatus.ACTIVE);
            entity.setMasteredAt(null);
        }
        entity.setUpdatedAt(now);
        wrongQuestionMapper.updateById(entity);
    }

    @Transactional(readOnly = true)
    public List<WrongQuestionEntity> listForUser(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        return wrongQuestionMapper.selectList(
                Wrappers.<WrongQuestionEntity>lambdaQuery()
                        .eq(WrongQuestionEntity::getUserId, userId)
                        .orderByDesc(WrongQuestionEntity::getLastWrongAt)
                        .orderByDesc(WrongQuestionEntity::getId));
    }

    @Transactional(readOnly = true)
    public Optional<WrongQuestionEntity> find(long userId, long questionId) {
        validateIds(userId, questionId);
        return Optional.ofNullable(select(userId, questionId));
    }

    private WrongQuestionEntity select(long userId, long questionId) {
        return wrongQuestionMapper.selectOne(
                Wrappers.<WrongQuestionEntity>lambdaQuery()
                        .eq(WrongQuestionEntity::getUserId, userId)
                        .eq(WrongQuestionEntity::getQuestionId, questionId));
    }

    private void validateIds(long userId, long questionId) {
        if (userId <= 0 || questionId <= 0) {
            throw new IllegalArgumentException("userId and questionId must be positive");
        }
    }
}
