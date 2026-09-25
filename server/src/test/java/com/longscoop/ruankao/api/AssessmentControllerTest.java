package com.longscoop.ruankao.api;

import com.longscoop.ruankao.assessment.AssessmentService;
import com.longscoop.ruankao.assessment.InsufficientPublishedQuestionsException;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.question.QuestionSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentControllerTest {

    @Test
    void enoughPublishedQuestionsStillCreatesAssessment() {
        AssessmentService service = mock(AssessmentService.class);
        UUID sessionId = UUID.randomUUID();
        when(service.start(2L, 1L, 20)).thenReturn(sessionId);
        var controller = new AssessmentController(service, mock(QuestionSessionService.class));

        var response = controller.start(
                new RuankaoPrincipal(2L),
                new AssessmentController.StartAssessmentRequest(1L, 20));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(new AssessmentController.StartAssessmentResponse(sessionId), response.getBody());
    }

    @Test
    void insufficientPublishedQuestionsReturnsConflictWithCounts() {
        AssessmentService service = mock(AssessmentService.class);
        when(service.start(2L, 1L, 20))
                .thenThrow(new InsufficientPublishedQuestionsException(20, 0));
        var controller = new AssessmentController(service, mock(QuestionSessionService.class));

        var response = controller.start(
                new RuankaoPrincipal(2L),
                new AssessmentController.StartAssessmentRequest(1L, 20));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(new AssessmentController.StartAssessmentUnavailableResponse(
                "INSUFFICIENT_PUBLISHED_QUESTIONS",
                "已发布题目不足，暂时无法开始摸底，请先跳过",
                20,
                0), response.getBody());
    }
}
