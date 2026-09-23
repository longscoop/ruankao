import { apiRequest } from '@/api/client'
import type {
  AnswerConfidence,
  AssessmentAnswerResponse,
  AssessmentQuestionDto,
  AssessmentSubmitResponse,
  StartAssessmentResponse,
} from '@/types/api'

export function startAssessment(examId: number, questionCount = 20): Promise<StartAssessmentResponse> {
  return apiRequest<StartAssessmentResponse>({
    path: '/api/v1/assessments',
    method: 'POST',
    data: { examId, questionCount },
  })
}

export function listAssessmentQuestions(sessionId: string): Promise<AssessmentQuestionDto[]> {
  return apiRequest<AssessmentQuestionDto[]>({ path: `/api/v1/assessments/${sessionId}/questions` })
}

export function submitAssessmentAnswer(sessionId: string, payload: {
  questionId: number
  answer: string
  durationSeconds?: number
  confidence?: AnswerConfidence
}): Promise<AssessmentAnswerResponse> {
  return apiRequest<AssessmentAnswerResponse>({
    path: `/api/v1/assessments/${sessionId}/answers`,
    method: 'POST',
    data: payload,
  })
}

export function finishAssessment(sessionId: string): Promise<AssessmentSubmitResponse> {
  return apiRequest<AssessmentSubmitResponse>({
    path: `/api/v1/assessments/${sessionId}/submit`,
    method: 'POST',
  })
}
