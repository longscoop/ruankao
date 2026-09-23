export interface AssessmentProgress {
  current: number
  total: number
  percent: number
}

export function assessmentProgress(index: number, total: number): AssessmentProgress {
  const safeTotal = Math.max(1, Math.floor(total || 1))
  const safeIndex = Math.min(Math.max(0, Math.floor(index || 0)), safeTotal - 1)
  const current = safeIndex + 1
  return { current, total: safeTotal, percent: Math.round((current / safeTotal) * 100) }
}

export function canSubmitAssessmentAnswer(questionId: number, answer: string): boolean {
  return Number.isInteger(questionId) && questionId > 0 && answer.trim().length > 0
}
