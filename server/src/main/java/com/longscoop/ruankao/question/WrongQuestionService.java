package com.longscoop.ruankao.question;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import com.longscoop.ruankao.question.persistence.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        wrongQuestionMapper.recordWrong(userId, questionId);
    }

    @Transactional
    public void recordCorrectReview(long userId, long questionId, double effectiveMastery) {
        validateIds(userId, questionId);
        if (!Double.isFinite(effectiveMastery)
                || effectiveMastery < 0.0
                || effectiveMastery > 100.0) {
            throw new IllegalArgumentException("effectiveMastery must be between 0 and 100");
        }

        wrongQuestionMapper.recordCorrectReview(userId, questionId, effectiveMastery);
    }

    @Transactional(readOnly = true)
    public Optional<WrongQuestionEntity> find(long userId, long questionId) {
        validateIds(userId, questionId);
        return Optional.ofNullable(
                wrongQuestionMapper.selectOne(
                        Wrappers.<WrongQuestionEntity>lambdaQuery()
                                .eq(WrongQuestionEntity::getUserId, userId)
                                .eq(WrongQuestionEntity::getQuestionId, questionId)));
    }

    private void validateIds(long userId, long questionId) {
        if (userId <= 0 || questionId <= 0) {
            throw new IllegalArgumentException("userId and questionId must be positive");
        }
    }
}
