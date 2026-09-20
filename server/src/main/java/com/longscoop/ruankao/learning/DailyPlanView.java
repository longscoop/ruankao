package com.longscoop.ruankao.learning;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanView(
        long planId,
        LocalDate planDate,
        int targetMinutes,
        List<StudyTaskView> tasks) {
}
