export interface StudyAgent { id: string; name: string; description: string }
export interface AgentSession { id: string; agentId: string; title: string; createdAt: string; updatedAt: string }
export interface AgentCitation { number: number; documentId: string; chunkId: number; filename: string; page: number; text: string }
export interface AgentTurn {
  id: number; requestId: string; question: string; content: string
  status: 'GROUNDED' | 'EXTRACT_ONLY' | 'NO_EVIDENCE'; citations: AgentCitation[]; createdAt: string
}
