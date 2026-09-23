package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.learning.DailyLearningService;
import com.longscoop.ruankao.learning.DailyPlanView;
import com.longscoop.ruankao.learning.StudyTaskView;
import com.longscoop.ruankao.learning.model.StudyTaskType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final DailyLearningService dailyLearningService;

    public LearningController(DailyLearningService dailyLearningService) {
        this.dailyLearningService = dailyLearningService;
    }

    @GetMapping("/today")
    public DailyPlanResponse today(@AuthenticationPrincipal RuankaoPrincipal principal) {
        return DailyPlanResponse.from(
                dailyLearningService.getOrCreateToday(principal.userId(), Clock.systemUTC()));
    }

    public record DailyPlanResponse(
            long planId,
            LocalDate planDate,
            int targetMinutes,
            List<StudyTaskResponse> tasks) {

        static DailyPlanResponse from(DailyPlanView plan) {
            return new DailyPlanResponse(
                    plan.planId(),
                    plan.planDate(),
                    plan.targetMinutes(),
                    plan.tasks().stream().map(StudyTaskResponse::from).toList());
        }
    }

    public record StudyTaskResponse(
            long taskId,
            StudyTaskType taskType,
            Long knowledgeId,
            int estimatedMinutes,
            Double priorityScore) {

        static StudyTaskResponse from(StudyTaskView task) {
            return new StudyTaskResponse(
                    task.taskId(),
                    task.taskType(),
                    task.knowledgeId(),
                    task.estimatedMinutes(),
                    task.priorityScore());
        }
    }
}
