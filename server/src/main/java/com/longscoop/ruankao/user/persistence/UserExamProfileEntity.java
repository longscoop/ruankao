package com.longscoop.ruankao.user.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.user.model.FoundationLevel;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@TableName("user_exam_profile")
public class UserExamProfileEntity {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Long examId;
    private LocalDate examDate;
    private Integer dailyTargetMinutes;
    private FoundationLevel foundationLevel;
    private Boolean assessmentCompleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Integer getDailyTargetMinutes() {
        return dailyTargetMinutes;
    }

    public void setDailyTargetMinutes(Integer dailyTargetMinutes) {
        this.dailyTargetMinutes = dailyTargetMinutes;
    }

    public FoundationLevel getFoundationLevel() {
        return foundationLevel;
    }

    public void setFoundationLevel(FoundationLevel foundationLevel) {
        this.foundationLevel = foundationLevel;
    }

    public Boolean getAssessmentCompleted() {
        return assessmentCompleted;
    }

    public void setAssessmentCompleted(Boolean assessmentCompleted) {
        this.assessmentCompleted = assessmentCompleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
