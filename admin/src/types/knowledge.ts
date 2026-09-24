export interface KnowledgeBase { id: string; examId: number; name: string; description: string }
export interface BaseInput { examId: number; name: string; description: string }
export interface ExamOption { id: number; name: string; code: string }
export interface MaterialDocument {
  id: string; baseId: string; filename: string; sha256: string; pageCount: number; chunkCount: number
  status: 'REVIEW' | 'PUBLISHED'; warnings: string[]; createdAt: string
}
export interface MaterialChunk { id: number; documentId: string; page: number; ordinal: number; text: string }
export interface AgentInput { name: string; description: string; instructions: string; baseIds: string[]; enabled: boolean }
export interface KnowledgeAgent extends AgentInput { id: string }
export interface AgentSession { id: string; agentId: string; title: string; createdAt: string; updatedAt: string }
export interface Citation { number: number; documentId: string; chunkId: number; filename: string; page: number; text: string }
export interface AgentTurn {
  id: number; requestId: string; question: string; content: string
  status: 'GROUNDED' | 'EXTRACT_ONLY' | 'NO_EVIDENCE'; citations: Citation[]; createdAt: string
}
