package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.learning.DailyLearningService;
import com.longscoop.ruankao.learning.DailyPlanView;
import com.longscoop.ruankao.learning.StudyTaskView;
import com.longscoop.ruankao.learning.model.StudyTaskType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final DailyLearningService dailyLearningService;

    public LearningController(DailyLearningService dailyLearningService) {
        this.dailyLearningService = dailyLearningService;
    }

    @GetMapping("/today")
    public ResponseEntity<?> today(@AuthenticationPrincipal RuankaoPrincipal principal) {
        try {
            return ResponseEntity.ok(DailyPlanResponse.from(
                    dailyLearningService.getOrCreateToday(principal.userId(), Clock.systemUTC())));
        } catch (IllegalArgumentException e) {
            if (!"user exam profile not found".equals(e.getMessage())) {
                throw e;
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", "EXAM_PROFILE_REQUIRED",
                    "message", "请先设置目标考试"));
        }
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
