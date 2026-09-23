import type { QuestionType } from '@/types/api'

export function toggleChoice(selected: string[], choice: string, type: QuestionType): string[] {
  if (type === 'SINGLE_CHOICE') return [choice]
  if (type !== 'MULTIPLE_CHOICE') return selected
  const unique = [...new Set(selected)]
  return unique.includes(choice) ? unique.filter((item) => item !== choice) : [...unique, choice]
}

export function buildAnswerValue(selected: string[], type: QuestionType): string {
  const unique = [...new Set(selected.map((item) => item.trim()).filter(Boolean))]
  return type === 'MULTIPLE_CHOICE' ? unique.sort().join(',') : (unique[0] || '')
}

export function toggleFavoriteIds(ids: number[], questionId: number): number[] {
  const unique = [...new Set(ids)]
  return unique.includes(questionId) ? unique.filter((id) => id !== questionId) : [...unique, questionId]
}
