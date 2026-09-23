import { api } from './client'
import type {
  BatchSummary,
  ImportDetail,
  QuestionKnowledgeLink,
} from '@/types/import'

export function listImports(examId?: number): Promise<BatchSummary[]> {
  const query = examId ? `?examId=${examId}` : ''
  return api<BatchSummary[]>(`/api/v1/admin/imports${query}`)
}

export function uploadPdf(examId: number, file: File) {
  const form = new FormData()
  form.append('examId', String(examId))
  form.append('file', file)
  return api<{
    batchId: string
    detectedType: string
    status: string
    pageCount: number
    itemCount: number
    issueCount: number
  }>('/api/v1/admin/imports/pdf', {
    method: 'POST',
    body: form,
  })
}

export function updateImportTitle(batchId: string, title: string) {
  return api<void>(`/api/v1/admin/imports/${batchId}`, {
    method: 'PUT',
    body: JSON.stringify({ title }),
  })
}

export function getImport(batchId: string) {
  return api<ImportDetail>(`/api/v1/admin/imports/${batchId}`)
}

export function getPagePreviewUrl(batchId: string, pageNumber: number) {
  return api<{ url: string }>(
    `/api/v1/admin/imports/${batchId}/pages/${pageNumber}/preview-url`,
  )
}

export function approveAll(batchId: string) {
  return api<void>(`/api/v1/admin/imports/${batchId}/approve-all`, { method: 'POST' })
}

export function approveItem(batchId: string, itemId: number) {
  return api<void>(`/api/v1/admin/imports/${batchId}/items/${itemId}/approve`, { method: 'POST' })
}

export function rejectItem(batchId: string, itemId: number) {
  return api<void>(`/api/v1/admin/imports/${batchId}/items/${itemId}/reject`, { method: 'POST' })
}

export function updateItem(batchId: string, itemId: number, title: string, contentJson: string) {
  return api<void>(`/api/v1/admin/imports/${batchId}/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify({ title, contentJson }),
  })
}

export function aiSuggest(batchId: string, itemId: number) {
  return api<{ requestId: string; content: string }>(
    `/api/v1/admin/imports/${batchId}/items/${itemId}/ai-suggest`,
    { method: 'POST' },
  )
}

export function resolveIssue(batchId: string, issueId: number) {
  return api<void>(`/api/v1/admin/imports/${batchId}/issues/${issueId}/resolve`, { method: 'POST' })
}

export function confirmImport(batchId: string, confirmKey: string) {
  return api<{ batchId: string; courseId?: number | null }>(
    `/api/v1/admin/imports/${batchId}/confirm`,
    { method: 'POST', body: JSON.stringify({ confirmKey }) },
  )
}

export function publishLesson(batchId: string, itemId: number, knowledgeIds: number[]) {
  return api<void>(`/api/v1/admin/imports/${batchId}/items/${itemId}/publish-lesson`, {
    method: 'POST',
    body: JSON.stringify({ knowledgeIds }),
  })
}

export function publishKnowledge(
  batchId: string,
  itemId: number,
  payload: {
    code: string
    parentId?: number | null
    importance: number
    examFrequency: number
    estimatedMinutes: number
    sortOrder: number
  },
) {
  return api<{ id: number }>(`/api/v1/admin/imports/${batchId}/items/${itemId}/publish-knowledge`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function publishQuestion(
  batchId: string,
  itemId: number,
  payload: {
    difficulty: 'EASY' | 'MEDIUM' | 'HARD'
    source: 'REAL_EXAM' | 'CHAPTER' | 'SIMULATION' | 'MANUAL'
    knowledgeLinks: QuestionKnowledgeLink[]
  },
) {
  return api<{ id: number }>(`/api/v1/admin/imports/${batchId}/items/${itemId}/publish-question`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
