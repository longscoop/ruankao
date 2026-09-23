package com.longscoop.ruankao.question.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.question.model.PracticeSource;

import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("question_session")
public class QuestionSessionEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;
    private Long userId;
    private Long examId;
    private PracticeSource source;
    private Long knowledgeId;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public PracticeSource getSource() { return source; }
    public void setSource(PracticeSource source) { this.source = source; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
