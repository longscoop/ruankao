package com.longscoop.ruankao.user;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.model.FoundationLevel;
import com.longscoop.ruankao.user.persistence.UserExamProfileEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserExamProfileServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserExamProfileService profileService;

    @Test
    void createsAndLoadsProfile() {
        long examId = examService.create("profile-create", "Profile Exam", ExamStatus.ACTIVE);
        LocalDate examDate = LocalDate.now().plusDays(60);

        profileService.upsert(1001L, examId, examDate, 30, FoundationLevel.SOME);

        UserExamProfileEntity profile = profileService.find(1001L).orElseThrow();
        assertEquals(1001L, profile.getUserId());
        assertEquals(examId, profile.getExamId());
        assertEquals(examDate, profile.getExamDate());
        assertEquals(30, profile.getDailyTargetMinutes());
        assertEquals(FoundationLevel.SOME, profile.getFoundationLevel());
        assertEquals(false, profile.getAssessmentCompleted());
    }

    @Test
    void repeatedUpsertUpdatesSameUserRow() {
        long firstExam = examService.create("profile-first", "First", ExamStatus.ACTIVE);
        long secondExam = examService.create("profile-second", "Second", ExamStatus.ACTIVE);

        profileService.upsert(
                1002L,
                firstExam,
                LocalDate.now().plusDays(30),
                15,
                FoundationLevel.ZERO);
        profileService.upsert(
                1002L,
                secondExam,
                LocalDate.now().plusDays(45),
                60,
                FoundationLevel.REVIEWING);

        UserExamProfileEntity profile = profileService.find(1002L).orElseThrow();
        assertEquals(secondExam, profile.getExamId());
        assertEquals(60, profile.getDailyTargetMinutes());
        assertEquals(FoundationLevel.REVIEWING, profile.getFoundationLevel());
        assertEquals(1L, profileService.countForUser(1002L));
    }

    @Test
    void allowsExamDateToday() {
        long examId = examService.create("profile-today", "Today", ExamStatus.ACTIVE);

        profileService.upsert(
                1003L,
                examId,
                LocalDate.now(),
                90,
                FoundationLevel.SOME);

        assertEquals(LocalDate.now(), profileService.find(1003L).orElseThrow().getExamDate());
    }

    @Test
    void rejectsInvalidProfileArguments() {
        long examId = examService.create("profile-validation", "Validation", ExamStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () ->
                profileService.upsert(0L, examId, LocalDate.now().plusDays(1), 30, FoundationLevel.SOME));
        assertThrows(IllegalArgumentException.class, () ->
                profileService.upsert(1004L, 999999999L, LocalDate.now().plusDays(1), 30, FoundationLevel.SOME));
        assertThrows(IllegalArgumentException.class, () ->
                profileService.upsert(1004L, examId, LocalDate.now().minusDays(1), 30, FoundationLevel.SOME));
        assertThrows(IllegalArgumentException.class, () ->
                profileService.upsert(1004L, examId, LocalDate.now().plusDays(1), 20, FoundationLevel.SOME));
        assertThrows(IllegalArgumentException.class, () ->
                profileService.upsert(1004L, examId, LocalDate.now().plusDays(1), 30, null));
    }
}
