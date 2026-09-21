import { apiRequest } from '@/api/client'
import type { AiChatResponse } from '@/types/api'

export function askAi(message: string): Promise<AiChatResponse> {
  return apiRequest<AiChatResponse>({ path: '/api/v1/ai/chat', method: 'POST', data: { message } })
}

export function explainQuestion(questionId: number): Promise<AiChatResponse> {
  return apiRequest<AiChatResponse>({ path: '/api/v1/ai/question-explain', method: 'POST', data: { questionId } })
}
