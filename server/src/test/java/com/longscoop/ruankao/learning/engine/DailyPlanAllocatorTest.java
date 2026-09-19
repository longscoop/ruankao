package com.longscoop.ruankao.learning.engine;

import com.longscoop.ruankao.learning.model.DailyPlanAllocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyPlanAllocatorTest {

    private final DailyPlanAllocator allocator = new DailyPlanAllocator();

    @Test
    void allocatesThirtyMinutesInNormalStage() {
        DailyPlanAllocation allocation = allocator.allocate(30, 30, true);

        assertEquals(new DailyPlanAllocation(6, 11, 7, 6), allocation);
    }

    @Test
    void allocatesSixtyMinutesInNormalStageWithoutRoundingLoss() {
        DailyPlanAllocation allocation = allocator.allocate(60, 30, true);

        assertEquals(new DailyPlanAllocation(12, 21, 15, 12), allocation);
    }

    @Test
    void switchesToExamSprintRatiosAtFourteenDays() {
        DailyPlanAllocation sprint = allocator.allocate(30, 14, true);
        DailyPlanAllocation normal = allocator.allocate(30, 15, true);

        assertEquals(new DailyPlanAllocation(9, 11, 3, 7), sprint);
        assertEquals(new DailyPlanAllocation(6, 11, 7, 6), normal);
    }

    @Test
    void movesWrongReviewMinutesToWeakPointsWhenNoWrongQuestionsAreDue() {
        DailyPlanAllocation allocation = allocator.allocate(30, 30, false);

        assertEquals(new DailyPlanAllocation(0, 17, 7, 6), allocation);
    }

    @Test
    void preservesTargetMinutesForEverySupportedTarget() {
        for (int target : new int[]{15, 30, 60, 90}) {
            assertEquals(target, allocator.allocate(target, 30, true).totalMinutes());
            assertEquals(target, allocator.allocate(target, 14, true).totalMinutes());
            assertEquals(target, allocator.allocate(target, 30, false).totalMinutes());
        }
    }

    @Test
    void largestRemainderTieBreakingIsDeterministic() {
        DailyPlanAllocation normal = allocator.allocate(90, 30, true);
        DailyPlanAllocation sprint = allocator.allocate(15, 14, true);

        assertEquals(new DailyPlanAllocation(18, 32, 22, 18), normal);
        assertEquals(new DailyPlanAllocation(5, 5, 1, 4), sprint);
    }

    @Test
    void rejectsUnsupportedTargetMinutes() {
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(20, 30, true));
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(0, 30, true));
    }

    @Test
    void rejectsNegativeDaysUntilExam() {
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(30, -1, true));
    }
}
