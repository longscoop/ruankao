import { apiRequest } from '@/api/client'
import type { ExamDto, ExamProfileDto, FoundationLevel } from '@/types/api'

export function listExams(): Promise<ExamDto[]> {
  return apiRequest<ExamDto[]>({ path: '/api/v1/exams' })
}

export function getExamProfile(): Promise<ExamProfileDto> {
  return apiRequest<ExamProfileDto>({ path: '/api/v1/users/me/exam-profile' })
}

export function updateExamProfile(payload: {
  examId: number
  examDate: string
  dailyTargetMinutes: number
  foundationLevel: FoundationLevel
}): Promise<void> {
  return apiRequest<void>({
    path: '/api/v1/users/me/exam-profile',
    method: 'PUT',
    data: payload,
  })
}
