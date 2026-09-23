package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserExamProfileController {

    private final UserExamProfileService profileService;

    public UserExamProfileController(UserExamProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/exam-profile")
    public ExamProfileResponse getProfile(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        var profile = profileService.find(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "exam profile not found"));
        return new ExamProfileResponse(
                profile.getExamId(),
                profile.getExamDate(),
                profile.getDailyTargetMinutes(),
                profile.getFoundationLevel(),
                Boolean.TRUE.equals(profile.getAssessmentCompleted()));
    }

    @PutMapping("/exam-profile")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestBody UpdateExamProfileRequest request) {
        profileService.upsert(
                principal.userId(),
                request.examId(),
                request.examDate(),
                request.dailyTargetMinutes(),
                request.foundationLevel());
        return ResponseEntity.noContent().build();
    }

    public record ExamProfileResponse(
            long examId,
            LocalDate examDate,
            int dailyTargetMinutes,
            FoundationLevel foundationLevel,
            boolean assessmentCompleted) {
    }

    public record UpdateExamProfileRequest(
            long examId,
            LocalDate examDate,
            int dailyTargetMinutes,
            FoundationLevel foundationLevel) {
    }
}
