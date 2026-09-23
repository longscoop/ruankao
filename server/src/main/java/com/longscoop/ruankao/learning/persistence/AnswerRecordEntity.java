package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;

import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("answer_record")
public class AnswerRecordEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;
    private Long userId;
    private Long questionId;
    private UUID sessionId;
    private String answer;
    private Boolean correct;
    private Integer durationSeconds;
    private AnswerConfidence confidence;
    private AnswerSource source;
    private String idempotencyKey;
    private Boolean masteryApplied;
    private OffsetDateTime answeredAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public AnswerConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(AnswerConfidence confidence) {
        this.confidence = confidence;
    }

    public AnswerSource getSource() {
        return source;
    }

    public void setSource(AnswerSource source) {
        this.source = source;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Boolean getMasteryApplied() {
        return masteryApplied;
    }

    public void setMasteryApplied(Boolean masteryApplied) {
        this.masteryApplied = masteryApplied;
    }

    public OffsetDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(OffsetDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
