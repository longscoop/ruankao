import { apiRequest } from '@/api/client'
import type { AnswerConfidence, PracticeQuestionDto, PracticeSource, QuestionAnswerFeedbackDto, QuestionSessionStartResponse, WrongQuestionDto } from '@/types/api'

export function startQuestionSession(payload: { source: PracticeSource; knowledgeId?: number }): Promise<QuestionSessionStartResponse> {
  return apiRequest<QuestionSessionStartResponse>({ path: '/api/v1/question-sessions', method: 'POST', data: payload })
}

export function listSessionQuestions(sessionId: string): Promise<PracticeQuestionDto[]> {
  return apiRequest<PracticeQuestionDto[]>({ path: `/api/v1/question-sessions/${sessionId}/questions` })
}

export function submitQuestionAnswer(sessionId: string, payload: { questionId: number; answer: string; durationSeconds?: number; confidence?: AnswerConfidence; source: PracticeSource }): Promise<QuestionAnswerFeedbackDto> {
  return apiRequest<QuestionAnswerFeedbackDto>({ path: `/api/v1/question-sessions/${sessionId}/answers`, method: 'POST', data: payload })
}

export function listWrongQuestions(): Promise<WrongQuestionDto[]> {
  return apiRequest<WrongQuestionDto[]>({ path: '/api/v1/users/me/wrong-questions' })
}
