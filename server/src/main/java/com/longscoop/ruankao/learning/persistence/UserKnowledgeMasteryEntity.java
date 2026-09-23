package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("user_knowledge_mastery")
public class UserKnowledgeMasteryEntity {

    private Long userId;
    private Long knowledgeId;
    private Double masteryScore;
    private Integer evidenceCount;
    private Integer correctStreak;
    private Integer wrongStreak;
    private OffsetDateTime lastEffectiveStudyAt;
    private OffsetDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Double getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(Double masteryScore) {
        this.masteryScore = masteryScore;
    }

    public Integer getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public Integer getCorrectStreak() {
        return correctStreak;
    }

    public void setCorrectStreak(Integer correctStreak) {
        this.correctStreak = correctStreak;
    }

    public Integer getWrongStreak() {
        return wrongStreak;
    }

    public void setWrongStreak(Integer wrongStreak) {
        this.wrongStreak = wrongStreak;
    }

    public OffsetDateTime getLastEffectiveStudyAt() {
        return lastEffectiveStudyAt;
    }

    public void setLastEffectiveStudyAt(OffsetDateTime lastEffectiveStudyAt) {
        this.lastEffectiveStudyAt = lastEffectiveStudyAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
