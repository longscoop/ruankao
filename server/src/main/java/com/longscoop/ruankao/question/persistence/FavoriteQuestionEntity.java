package com.longscoop.ruankao.question.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("favorite_question")
public class FavoriteQuestionEntity {

    private Long userId;
    private Long questionId;
    private OffsetDateTime createdAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
