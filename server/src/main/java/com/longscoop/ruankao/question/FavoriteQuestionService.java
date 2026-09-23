package com.longscoop.ruankao.question;

import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.persistence.FavoriteQuestionEntity;
import com.longscoop.ruankao.question.persistence.FavoriteQuestionMapper;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.user.UserExamProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteQuestionService {

    private final FavoriteQuestionMapper favoriteMapper;
    private final QuestionService questionService;
    private final UserExamProfileService profileService;

    public FavoriteQuestionService(
            FavoriteQuestionMapper favoriteMapper,
            QuestionService questionService,
            UserExamProfileService profileService) {
        this.favoriteMapper = favoriteMapper;
        this.questionService = questionService;
        this.profileService = profileService;
    }

    @Transactional
    public void add(long userId, long questionId) {
        QuestionEntity question = requireVisibleQuestion(userId, questionId);
        favoriteMapper.add(userId, question.getId());
    }

    @Transactional
    public void remove(long userId, long questionId) {
        favoriteMapper.remove(userId, questionId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteQuestionEntity> list(long userId) {
        return favoriteMapper.list(userId);
    }

    private QuestionEntity requireVisibleQuestion(long userId, long questionId) {
        QuestionEntity question = questionService.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("question not found"));
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException("question is not published");
        }
        long examId = profileService.find(userId)
                .orElseThrow(() -> new IllegalStateException("exam profile is required"))
                .getExamId();
        if (!question.getExamId().equals(examId)) {
            throw new IllegalArgumentException("question does not belong to user exam");
        }
        return question;
    }
}
