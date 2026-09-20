package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.learning.model.StudyTaskStatus;
import com.longscoop.ruankao.learning.model.StudyTaskType;

import java.time.OffsetDateTime;

@TableName("study_task")
public class StudyTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private StudyTaskType taskType;
    private Long knowledgeId;
    private Integer targetQuestionCount;
    private Integer completedQuestionCount;
    private Integer estimatedMinutes;
    private Integer actualMinutes;
    private Double priorityScore;
    private Integer sortOrder;
    private StudyTaskStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public StudyTaskType getTaskType() { return taskType; }
    public void setTaskType(StudyTaskType taskType) { this.taskType = taskType; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public Integer getTargetQuestionCount() { return targetQuestionCount; }
    public void setTargetQuestionCount(Integer targetQuestionCount) { this.targetQuestionCount = targetQuestionCount; }
    public Integer getCompletedQuestionCount() { return completedQuestionCount; }
    public void setCompletedQuestionCount(Integer completedQuestionCount) { this.completedQuestionCount = completedQuestionCount; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer actualMinutes) { this.actualMinutes = actualMinutes; }
    public Double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public StudyTaskStatus getStatus() { return status; }
    public void setStatus(StudyTaskStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
