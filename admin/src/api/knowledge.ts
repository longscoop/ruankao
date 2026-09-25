import { api } from '@/api/client'
import type { KnowledgeBase, BaseInput, ExamOption, MaterialDocument, MaterialChunk, AgentInput,
  KnowledgeAgent, AgentSession, AgentTurn, Citation } from '@/types/knowledge'

const root = '/api/v1/admin/knowledge'
const learner = '/api/v1/ai/agents'
const json = (method: string, value: unknown): RequestInit => ({ method, body: JSON.stringify(value) })
export const listBases = () => api<KnowledgeBase[]>(`${root}/bases`)
export const listKnowledgeExams = () => api<ExamOption[]>('/api/v1/exams')
export const saveBase = (id: string | undefined, input: BaseInput) => api<KnowledgeBase>(id ? `${root}/bases/${id}` : `${root}/bases`, json(id ? 'PUT' : 'POST', input))
export const removeBase = (id: string) => api<void>(`${root}/bases/${id}`, { method: 'DELETE' })
export const listDocuments = (id: string, offset = 0) => api<MaterialDocument[]>(`${root}/bases/${id}/documents?offset=${offset}&limit=50`)
export function uploadMaterial(id: string, file: File): Promise<MaterialDocument> {
  const body = new FormData(); body.append('file', file)
  return api<MaterialDocument>(`${root}/bases/${id}/documents`, { method: 'POST', body })
}
export const listChunks = (id: string, offset = 0) => api<MaterialChunk[]>(`${root}/documents/${id}/chunks?offset=${offset}&limit=50`)
export const publishDocument = (id: string, published: boolean) => api<MaterialDocument>(`${root}/documents/${id}/publication`, json('PUT', { published }))
export const removeDocument = (id: string) => api<void>(`${root}/documents/${id}`, { method: 'DELETE' })
export const listAgents = () => api<KnowledgeAgent[]>(`${root}/agents`)
export const saveAgent = (id: string | undefined, input: AgentInput) => api<KnowledgeAgent>(id ? `${root}/agents/${id}` : `${root}/agents`, json(id ? 'PUT' : 'POST', input))
export const removeAgent = (id: string) => api<void>(`${root}/agents/${id}`, { method: 'DELETE' })
export const listAgentSessions = (id: string, offset = 0) => api<AgentSession[]>(`${learner}/${id}/sessions?offset=${offset}&limit=50`)
export const startAgentSession = (id: string) => api<AgentSession>(`${learner}/${id}/sessions`, { method: 'POST' })
export const listAgentTurns = (id: string, offset = 0) => api<AgentTurn[]>(`${learner}/sessions/${id}/turns?offset=${offset}&limit=100`)
export const askAgent = (id: string, requestId: string, message: string) => api<AgentTurn>(`${learner}/sessions/${id}/turns`, json('POST', { requestId, message }))
export const getAgentSource = (id: string, chunkId: number) => api<Citation>(`${learner}/${id}/sources/${chunkId}`)
