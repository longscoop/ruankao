import { api } from './client'

export interface ExamSummary {
  id: number
  code: string
  name: string
}

export function listActiveExams(): Promise<ExamSummary[]> {
  return api<ExamSummary[]>('/api/v1/exams')
}
