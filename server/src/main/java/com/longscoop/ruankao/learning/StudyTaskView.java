package com.longscoop.ruankao.learning;

import com.longscoop.ruankao.learning.model.StudyTaskType;

public record StudyTaskView(
        long taskId,
        StudyTaskType taskType,
        Long knowledgeId,
        int estimatedMinutes,
        Double priorityScore) {
}
