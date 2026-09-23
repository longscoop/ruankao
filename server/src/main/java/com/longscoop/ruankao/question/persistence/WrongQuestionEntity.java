package com.longscoop.ruankao.question.persistence;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.question.model.WrongQuestionStatus;

import java.time.OffsetDateTime;

@TableName("wrong_question")
public class WrongQuestionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private WrongQuestionStatus status;
    private Integer wrongCount;
    private Integer consecutiveCorrect;
    private OffsetDateTime firstWrongAt;
    private OffsetDateTime lastWrongAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime masteredAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public WrongQuestionStatus getStatus() {
        return status;
    }

    public void setStatus(WrongQuestionStatus status) {
        this.status = status;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Integer getConsecutiveCorrect() {
        return consecutiveCorrect;
    }

    public void setConsecutiveCorrect(Integer consecutiveCorrect) {
        this.consecutiveCorrect = consecutiveCorrect;
    }

    public OffsetDateTime getFirstWrongAt() {
        return firstWrongAt;
    }

    public void setFirstWrongAt(OffsetDateTime firstWrongAt) {
        this.firstWrongAt = firstWrongAt;
    }

    public OffsetDateTime getLastWrongAt() {
        return lastWrongAt;
    }

    public void setLastWrongAt(OffsetDateTime lastWrongAt) {
        this.lastWrongAt = lastWrongAt;
    }

    public OffsetDateTime getMasteredAt() {
        return masteredAt;
    }

    public void setMasteredAt(OffsetDateTime masteredAt) {
        this.masteredAt = masteredAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
