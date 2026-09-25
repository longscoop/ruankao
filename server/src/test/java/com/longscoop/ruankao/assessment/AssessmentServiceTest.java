package com.longscoop.ruankao.assessment;

import com.longscoop.ruankao.assessment.persistence.AssessmentItemMapper;
import com.longscoop.ruankao.assessment.persistence.AssessmentSessionMapper;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeMapper;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.persistence.UserExamProfileEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentServiceTest {

    @Test
    void emptyPublishedQuestionBankReportsRequiredAndAvailableCounts() {
        QuestionMapper questions = mock(QuestionMapper.class);
        UserExamProfileService profiles = mock(UserExamProfileService.class);
        UserExamProfileEntity profile = new UserExamProfileEntity();
        profile.setExamId(1L);
        when(profiles.find(2L)).thenReturn(Optional.of(profile));
        when(questions.selectList(any())).thenReturn(List.of());
        var service = new AssessmentService(
                mock(AssessmentSessionMapper.class),
                mock(AssessmentItemMapper.class),
                questions,
                mock(QuestionKnowledgeMapper.class),
                mock(MasteryApplicationService.class),
                profiles);

        var error = assertThrows(InsufficientPublishedQuestionsException.class,
                () -> service.start(2L, 1L, 20));

        assertEquals(20, error.requiredCount());
        assertEquals(0, error.availableCount());
    }
}
