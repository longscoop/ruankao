import type { ImportItem, QuestionKnowledgeLink } from '../types/import.ts'

export function selectExamId(
  current: number | undefined,
  exams: ReadonlyArray<{ id: number }>,
): number | undefined {
  return exams.find(exam => exam.id === current)?.id ?? exams[0]?.id
}

export function parseKnowledgeIds(value: string): number[] {
  const result: number[] = []
  for (const part of value.split(',')) {
    const id = Number(part.trim())
    if (Number.isInteger(id) && id > 0 && !result.includes(id)) {
      result.push(id)
    }
  }
  return result
}

export function parseQuestionLinks(value: string): QuestionKnowledgeLink[] {
  const ids = parseKnowledgeIds(value)
  if (ids.length !== 1) return []
  return [{ knowledgeId: ids[0], weight: 1, primary: true }]
}

export function itemNeedsConfirm(
  item: Pick<ImportItem, 'itemType' | 'status' | 'targetId'>,
): boolean {
  return item.itemType === 'LESSON'
    && item.status === 'APPROVED'
    && !item.targetId
}
