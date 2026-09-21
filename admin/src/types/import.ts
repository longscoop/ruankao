export type ImportDocumentType = 'QUESTION_BANK' | 'LECTURE' | 'MIXED' | 'UNKNOWN'
export type ImportBatchStatus = 'PARSED' | 'REVIEWING' | 'CONFIRMED' | 'PUBLISHED' | 'FAILED'
export type ImportItemType = 'LESSON' | 'KNOWLEDGE' | 'QUESTION'
export type ImportItemStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'MATERIALIZED' | 'PUBLISHED'
export type IssueSeverity = 'INFO' | 'WARNING' | 'ERROR'
export type IssueStatus = 'OPEN' | 'RESOLVED'

export interface BatchSummary {
  batchId: string
  examId: number
  filename: string
  title?: string | null
  detectedType: ImportDocumentType
  status: ImportBatchStatus
  pageCount: number
  courseId?: number | null
  createdAt?: string | null
}

export interface ImportPage {
  pageNumber: number
  textContent?: string | null
  imageObjectKey?: string | null
}

export interface ImportItem {
  id: number
  itemType: ImportItemType
  itemKey: string
  sourcePageStart: number
  sourcePageEnd: number
  title?: string | null
  contentJson: string
  status: ImportItemStatus
  targetId?: number | null
}

export interface ImportIssue {
  id: number
  itemId?: number | null
  severity: IssueSeverity
  code: string
  message: string
  sourcePage?: number | null
  status: IssueStatus
}

export interface ImportDetail extends BatchSummary {
  pages: ImportPage[]
  items: ImportItem[]
  issues: ImportIssue[]
}

export interface QuestionKnowledgeLink {
  knowledgeId: number
  weight: number
  primary: boolean
}
