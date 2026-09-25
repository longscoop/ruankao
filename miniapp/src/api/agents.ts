import { apiRequest } from '@/api/client'
import type { StudyAgent, AgentSession, AgentTurn, AgentCitation } from '@/types/agents'
const root = '/api/v1/ai/agents'
export const listStudyAgents = () => apiRequest<StudyAgent[]>({ path: root })
export const listAgentSessions = (id: string, offset = 0) => apiRequest<AgentSession[]>({ path: `${root}/${id}/sessions?offset=${offset}&limit=50` })
export const startAgentSession = (id: string) => apiRequest<AgentSession>({ path: `${root}/${id}/sessions`, method: 'POST' })
export const listAgentTurns = (id: string, offset = 0) => apiRequest<AgentTurn[]>({ path: `${root}/sessions/${id}/turns?offset=${offset}&limit=100` })
export const askStudyAgent = (id: string, requestId: string, message: string) => apiRequest<AgentTurn>({ path: `${root}/sessions/${id}/turns`, method: 'POST', data: { requestId, message }, timeout: 60000 })
export const getAgentSource = (id: string, chunkId: number) => apiRequest<AgentCitation>({ path: `${root}/${id}/sources/${chunkId}` })
