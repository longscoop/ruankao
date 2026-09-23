package com.longscoop.ruankao.learning.model;

public record DailyPlanAllocation(
        int wrongReviewMinutes,
        int weakPointMinutes,
        int newKnowledgeMinutes,
        int realExamMinutes) {

    public int totalMinutes() {
        return wrongReviewMinutes + weakPointMinutes + newKnowledgeMinutes + realExamMinutes;
    }

    public int minutesFor(StudyTaskType taskType) {
        if (taskType == null) {
            throw new IllegalArgumentException("taskType is required");
        }
        return switch (taskType) {
            case WRONG_REVIEW -> wrongReviewMinutes;
            case WEAK_POINT -> weakPointMinutes;
            case NEW_KNOWLEDGE -> newKnowledgeMinutes;
            case REAL_EXAM -> realExamMinutes;
        };
    }
}
