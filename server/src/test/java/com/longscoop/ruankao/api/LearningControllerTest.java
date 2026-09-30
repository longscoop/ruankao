package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.learning.DailyLearningService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningControllerTest {

    @Test
    void missingExamProfileReturnsNotFoundInsteadOfServerError() {
        DailyLearningService service = mock(DailyLearningService.class);
        when(service.getOrCreateToday(eq(2L), any(Clock.class)))
                .thenThrow(new IllegalArgumentException("user exam profile not found"));

        var response = new LearningController(service).today(new RuankaoPrincipal(2L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Map.of("code", "EXAM_PROFILE_REQUIRED", "message", "请先设置目标考试"), response.getBody());
    }
}
