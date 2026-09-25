package com.longscoop.ruankao.assessment;

public final class InsufficientPublishedQuestionsException extends IllegalStateException {

    private final int requiredCount;
    private final int availableCount;

    public InsufficientPublishedQuestionsException(int requiredCount, int availableCount) {
        super("insufficient published questions");
        this.requiredCount = requiredCount;
        this.availableCount = availableCount;
    }

    public int requiredCount() {
        return requiredCount;
    }

    public int availableCount() {
        return availableCount;
    }
}
