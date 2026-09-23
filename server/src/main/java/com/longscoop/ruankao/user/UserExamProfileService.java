package com.longscoop.ruankao.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.exam.persistence.ExamMapper;
import com.longscoop.ruankao.user.model.FoundationLevel;
import com.longscoop.ruankao.user.persistence.UserExamProfileEntity;
import com.longscoop.ruankao.user.persistence.UserExamProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
public class UserExamProfileService {

    private static final Set<Integer> SUPPORTED_TARGET_MINUTES = Set.of(15, 30, 60, 90);

    private final UserExamProfileMapper profileMapper;
    private final ExamMapper examMapper;

    public UserExamProfileService(
            UserExamProfileMapper profileMapper,
            ExamMapper examMapper) {
        this.profileMapper = profileMapper;
        this.examMapper = examMapper;
    }

    @Transactional
    public void upsert(
            long userId,
            long examId,
            LocalDate examDate,
            int dailyTargetMinutes,
            FoundationLevel foundationLevel) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (examId <= 0 || examMapper.selectById(examId) == null) {
            throw new IllegalArgumentException("exam must exist");
        }
        if (examDate == null || examDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("examDate cannot be in the past");
        }
        if (!SUPPORTED_TARGET_MINUTES.contains(dailyTargetMinutes)) {
            throw new IllegalArgumentException("dailyTargetMinutes must be one of 15, 30, 60, 90");
        }
        if (foundationLevel == null) {
            throw new IllegalArgumentException("foundationLevel is required");
        }

        profileMapper.upsert(userId, examId, examDate, dailyTargetMinutes, foundationLevel);
    }

    @Transactional(readOnly = true)
    public Optional<UserExamProfileEntity> find(long userId) {
        if (userId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(profileMapper.selectById(userId));
    }

    @Transactional
    public void markAssessmentCompleted(long userId, long examId) {
        int updated = profileMapper.update(
                null,
                Wrappers.<UserExamProfileEntity>lambdaUpdate()
                        .eq(UserExamProfileEntity::getUserId, userId)
                        .eq(UserExamProfileEntity::getExamId, examId)
                        .set(UserExamProfileEntity::getAssessmentCompleted, true));
        if (updated != 1) {
            throw new IllegalStateException("matching user exam profile not found");
        }
    }

    @Transactional(readOnly = true)
    public long countForUser(long userId) {
        if (userId <= 0) {
            return 0L;
        }
        return profileMapper.selectCount(
                Wrappers.<UserExamProfileEntity>lambdaQuery()
                        .eq(UserExamProfileEntity::getUserId, userId));
    }
}
