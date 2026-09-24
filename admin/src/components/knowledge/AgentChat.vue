<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { listAgentSessions, startAgentSession, listAgentTurns, askAgent, getAgentSource } from '@/api/knowledge'
import { prepareTurn, answerLabel, sourceLabel } from '@/lib/knowledge'
import type { PendingTurn } from '@/lib/knowledge'
import type { KnowledgeAgent, AgentSession, AgentTurn, Citation } from '@/types/knowledge'
const props = defineProps<{ agents: KnowledgeAgent[] }>()
const enabled = computed(() => props.agents.filter(agent => agent.enabled))
const agentId = ref('')
const sessions = ref<AgentSession[]>([])
const sessionId = ref('')
const turns = ref<AgentTurn[]>([])
const message = ref('')
const pending = ref<PendingTurn>()
const busy = ref(false)
const error = ref('')
const moreSessions = ref(false)
const source = ref<Citation>()
const sourceOpen = ref(false)
async function run(work: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await work() } catch (e) { error.value = (e as Error).message || '问答服务暂不可用' }
  finally { busy.value = false }
}
async function loadSessions(more = false) {
  if (!agentId.value) { sessions.value = []; return }
  const rows = await listAgentSessions(agentId.value, more ? sessions.value.length : 0)
  sessions.value = more ? [...sessions.value, ...rows] : rows; moreSessions.value = rows.length === 50
}
async function selectAgent() {
  sessionId.value = ''; turns.value = []; message.value = ''; pending.value = undefined; sessions.value = []
  await run(() => loadSessions())
}
watch(enabled, agents => {
  if (!agents.some(agent => agent.id === agentId.value)) { agentId.value = agents[0]?.id || ''; void selectAgent() }
}, { immediate: true })
async function openSession() {
  await run(async () => {
    turns.value = []; pending.value = undefined; message.value = ''
    if (!sessionId.value) return
    const first = await listAgentTurns(sessionId.value)
    const rest = first.length === 100 ? await listAgentTurns(sessionId.value, 100) : []
    turns.value = [...first, ...rest]
  })
}
async function start() {
  await run(async () => {
    if (!agentId.value) return
    const session = await startAgentSession(agentId.value)
    sessions.value = [session, ...sessions.value]; sessionId.value = session.id
    turns.value = []; pending.value = undefined; message.value = ''
  })
}
async function send() {
  await run(async () => {
    pending.value = prepareTurn(pending.value, sessionId.value, message.value)
    const result = await askAgent(sessionId.value, pending.value.requestId, pending.value.message)
    const found = turns.value.findIndex(turn => turn.id === result.id)
    if (found < 0) turns.value.push(result); else turns.value[found] = result
    pending.value = undefined; message.value = ''
  })
}
async function viewSource(citation: Citation) {
  await run(async () => {
    source.value = undefined; sourceOpen.value = true
    source.value = await getAgentSource(agentId.value, citation.chunkId)
  })
}
</script>
<template>
  <div class="chat">
    <el-alert title="此处使用当前管理员自己的会话，遵守与学员相同的资料发布、智能体启用和 AI 配额规则。" type="info" :closable="false" />
    <div class="toolbar">
      <el-select v-model="agentId" placeholder="选择已启用的智能体" :disabled="busy" @change="selectAgent"><el-option v-for="agent in enabled" :key="agent.id" :label="agent.name" :value="agent.id" /></el-select>
      <el-select v-model="sessionId" placeholder="选择历史会话" :disabled="busy || !agentId" @change="openSession"><el-option v-for="session in sessions" :key="session.id" :label="session.title" :value="session.id" /></el-select>
      <el-button :disabled="busy || !agentId" @click="start">新建会话</el-button>
      <el-button :disabled="busy || !agentId" @click="run(() => loadSessions())">刷新会话</el-button>
      <el-button v-if="moreSessions" :disabled="busy" @click="run(() => loadSessions(true))">更多历史</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-empty v-if="!enabled.length" description="请先在智能体配置中启用一个智能体。" />
    <el-empty v-else-if="!turns.length && !busy" :description="sessionId ? '提出一个资料中的问题，答案会显示引用。' : '新建或选择会话开始调试。'" />
    <article v-for="turn in turns" :key="turn.id" class="turn">
      <h4>{{ turn.question }}</h4><div class="status">{{ answerLabel(turn.status) }}</div><pre>{{ turn.content }}</pre>
      <details v-for="citation in turn.citations" :key="citation.chunkId" class="citation"><summary>[{{ citation.number }}] {{ sourceLabel(citation) }}</summary><p class="muted">回答时的原文快照；实时原文可能已被撤回。</p><pre>{{ citation.text }}</pre><el-button link type="primary" :disabled="busy" @click="viewSource(citation)">校验并查看当前原文</el-button></details>
    </article>
    <div v-if="agentId" class="composer"><el-input v-model="message" type="textarea" :rows="4" maxlength="2000" show-word-limit :disabled="busy || !sessionId" placeholder="输入资料问题，可继续追问；每个会话最多 200 轮。" /><div class="send"><span>失败后保留问题；内容不变再次发送会复用请求标识。</span><el-button type="primary" :loading="busy" :disabled="busy || !sessionId || !message.trim()" @click="send">{{ pending ? '重试发送' : '发送问题' }}</el-button></div></div>
    <el-dialog v-model="sourceOpen" title="当前可访问的原文" width="min(780px, 94vw)"><template v-if="source"><strong>{{ sourceLabel(source) }}</strong><pre>{{ source.text }}</pre></template><el-alert v-else-if="!busy" :title="error || '原文当前不可用'" type="warning" :closable="false" /></el-dialog>
  </div>
</template>
<style scoped>
.toolbar{display:flex;flex-wrap:wrap;gap:12px;margin:18px 0}.toolbar .el-select{width:250px}.turn{background:white;border:1px solid #e4e7ed;border-radius:12px;padding:22px;margin:18px 0}.turn h4{margin:0 0 14px;font-size:16px;line-height:1.6;overflow-wrap:anywhere}.status{font-size:12px;color:#52637e}.turn pre,pre{font-family:inherit;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.85;color:#172033}.citation{background:#f5f7fc;border-radius:8px;margin-top:10px;padding:12px 16px}.citation summary{cursor:pointer;font-size:13px;color:#345ec2}.muted{font-size:12px;color:#667085}.composer{margin-top:22px}.send{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-top:12px}.send span{font-size:12px;color:#667085}
</style>
