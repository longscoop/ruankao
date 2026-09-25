<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { listStudyAgents, listAgentSessions, startAgentSession, listAgentTurns, askStudyAgent, getAgentSource } from '@/api/agents'
import { prepareTurn, answerLabel, sourceLabel } from '@/lib/agents'
import type { PendingTurn } from '@/lib/agents'
import type { StudyAgent, AgentSession, AgentTurn, AgentCitation } from '@/types/agents'
const agents = ref<StudyAgent[]>([])
const agentId = ref('')
const sessions = ref<AgentSession[]>([])
const sessionId = ref('')
const turns = ref<AgentTurn[]>([])
const message = ref('')
const pending = ref<PendingTurn>()
const busy = ref(false)
const loaded = ref(false)
const error = ref('')
const hasMoreSessions = ref(false)
const source = ref<AgentCitation>()
const activeAgent = computed(() => agents.value.find(agent => agent.id === agentId.value))
const activeSession = computed(() => sessions.value.find(session => session.id === sessionId.value))
const agentIndex = computed(() => Math.max(0, agents.value.findIndex(agent => agent.id === agentId.value)))
const sessionIndex = computed(() => Math.max(0, sessions.value.findIndex(session => session.id === sessionId.value)))
async function run(work: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await work() }
  catch (e) { error.value = (e as { message?: string }).message || '服务暂不可用，请检查网络或登录状态后重试' }
  finally { busy.value = false }
}
function resetConversation() { sessionId.value = ''; turns.value = []; message.value = ''; pending.value = undefined; source.value = undefined }
async function loadSessions(more = false) {
  const rows = await listAgentSessions(agentId.value, more ? sessions.value.length : 0)
  sessions.value = more ? [...sessions.value, ...rows] : rows
  hasMoreSessions.value = rows.length === 50
}
async function openSession(id: string) {
  resetConversation(); sessionId.value = id
  const first = await listAgentTurns(id)
  const rest = first.length === 100 ? await listAgentTurns(id, 100) : []
  turns.value = [...first, ...rest]
}
async function refresh() {
  await run(async () => {
    agents.value = await listStudyAgents(); loaded.value = true
    if (!agents.value.some(agent => agent.id === agentId.value)) agentId.value = agents.value[0]?.id || ''
    resetConversation(); sessions.value = []
    if (agentId.value) { await loadSessions(); if (sessions.value[0]) await openSession(sessions.value[0].id) }
  })
}
async function chooseAgent(event: { detail: { value: unknown } }) {
  if (busy.value) return
  const agent = agents.value[Number(event.detail.value)]
  if (!agent) return
  await run(async () => {
    agentId.value = agent.id; resetConversation(); sessions.value = []; await loadSessions()
    if (sessions.value[0]) await openSession(sessions.value[0].id)
  })
}
async function chooseSession(event: { detail: { value: unknown } }) {
  const session = sessions.value[Number(event.detail.value)]
  if (session) await run(() => openSession(session.id))
}
async function start() {
  await run(async () => {
    if (!agentId.value) return
    const session = await startAgentSession(agentId.value)
    resetConversation(); sessionId.value = session.id; sessions.value = [session, ...sessions.value]
  })
}
async function send() {
  await run(async () => {
    pending.value = prepareTurn(pending.value, sessionId.value, message.value)
    const answer = await askStudyAgent(sessionId.value, pending.value.requestId, pending.value.message)
    const index = turns.value.findIndex(turn => turn.id === answer.id)
    if (index < 0) turns.value.push(answer); else turns.value[index] = answer
    pending.value = undefined; message.value = ''
  })
}
async function viewSource(citation: AgentCitation) {
  await run(async () => { source.value = undefined; source.value = await getAgentSource(agentId.value, citation.chunkId) })
}
onLoad(() => { void refresh() })
</script>
<template>
  <view class="ruankao-page agent-page">
    <text class="eyebrow">STUDY WITH SOURCES</text><text class="title">资料智能体</text>
    <text class="description">围绕已发布的软考资料提问，答案可追溯到原文。AI 仍可能误解资料，请结合来源核对。</text>
    <view v-if="error" class="error">{{ error }}</view>
    <view class="row"><button size="mini" :disabled="busy" :loading="busy" @tap="refresh">刷新</button><text v-if="busy" class="hint">正在处理，请勿重复发送</text></view>
    <view v-if="loaded && !agents.length" class="empty ruankao-card"><text>还没有可用的资料智能体。</text><text class="hint">管理员上传并发布资料、启用智能体后，即可在这里开始学习。</text></view>
    <template v-if="agents.length">
      <view class="selection ruankao-card">
        <text class="label">学习助教</text><picker :range="agents" range-key="name" :value="agentIndex" :disabled="busy" @change="chooseAgent"><view class="picker">{{ activeAgent?.name || '选择智能体' }} <text>⌄</text></view></picker>
        <text v-if="activeAgent?.description" class="hint">{{ activeAgent.description }}</text>
        <text class="label">历史会话</text><picker :range="sessions" range-key="title" :value="sessionIndex" :disabled="busy || !sessions.length" @change="chooseSession"><view class="picker">{{ activeSession?.title || '暂无会话，请新建' }} <text>⌄</text></view></picker>
        <view class="row"><button size="mini" :disabled="busy || !agentId" @tap="start">新建会话</button><button v-if="hasMoreSessions" size="mini" :disabled="busy" @tap="run(() => loadSessions(true))">更多历史</button></view>
      </view>
      <view v-if="sessionId && !turns.length && !busy" class="empty"><text>可以开始提问了，例如询问资料中的概念、区别或易错点。</text></view>
      <view v-for="turn in turns" :key="turn.id" class="turn ruankao-card">
        <text class="question">{{ turn.question }}</text><text class="answer-status">{{ answerLabel(turn.status) }}</text><text class="answer" selectable>{{ turn.content }}</text>
        <view v-for="citation in turn.citations" :key="citation.chunkId" class="citation">
          <text class="source-title">[{{ citation.number }}] {{ sourceLabel(citation) }}</text>
          <text class="hint">回答时的原文快照</text><text class="source-text" selectable>{{ citation.text }}</text>
          <button size="mini" :disabled="busy" @tap="viewSource(citation)">校验并查看当前原文</button>
        </view>
      </view>
      <view v-if="source" class="source-current ruankao-card"><view class="row"><text class="source-title">当前原文：{{ sourceLabel(source) }}</text><button size="mini" @tap="source = undefined">收起</button></view><text class="answer" selectable>{{ source.text }}</text></view>
      <view class="composer ruankao-card"><textarea v-model="message" :disabled="busy || !sessionId" :maxlength="2000" class="input" placeholder="输入资料问题，可继续追问…" /><text class="hint">{{ message.length }}/2000 · 每个会话最多 200 轮</text><button class="primary" :loading="busy" :disabled="busy || !sessionId || !message.trim()" @tap="send">{{ pending ? '重试发送' : '发送问题' }}</button><text class="hint">发送失败会保留问题；内容不变直接重试，不会重复保存已成功的问答。</text></view>
    </template>
  </view>
</template>
<style scoped>
.agent-page{padding-bottom:60rpx}.eyebrow{color:#6554bc;font-size:21rpx;font-weight:700;letter-spacing:3rpx}.title{display:block;font-size:46rpx;font-weight:800;margin:12rpx 0}.description{display:block;color:#667085;font-size:25rpx;line-height:1.75}.selection,.turn,.composer,.source-current{margin-top:24rpx;padding:28rpx}.label{display:block;margin:10rpx 0 16rpx;color:#52637e;font-size:23rpx}.picker{display:flex;justify-content:space-between;padding:22rpx;background:#f4f6fb;border-radius:16rpx;font-size:27rpx;color:#172033}.hint{display:block;color:#667085;font-size:22rpx;line-height:1.7;margin-top:12rpx}.row{display:flex;gap:16rpx;align-items:center;flex-wrap:wrap;margin:16rpx 0}.row button{margin:0}.empty{padding:34rpx;margin-top:22rpx;line-height:1.8;color:#667085;font-size:26rpx}.question{display:block;font-size:29rpx;font-weight:750;line-height:1.7;word-break:break-all}.answer-status{display:block;font-size:21rpx;color:#6554bc;margin:18rpx 0}.answer,.source-text{display:block;white-space:pre-wrap;word-break:break-all;line-height:1.85;color:#172033;font-size:26rpx}.citation{padding:22rpx;background:#f5f7fc;border-radius:16rpx;margin-top:20rpx}.source-title{display:block;font-size:23rpx;color:#345ec2;line-height:1.7;word-break:break-all}.source-text{font-size:23rpx;margin:12rpx 0 18rpx}.citation button{font-size:22rpx;margin:0}.input{width:100%;min-height:220rpx;box-sizing:border-box;font-size:27rpx;line-height:1.7}.primary{margin-top:18rpx;background:#172033;color:white;border-radius:18rpx;font-weight:700}.error{margin-top:20rpx;padding:24rpx;border-radius:16rpx;background:#fff2e9;color:#8b4700;font-size:25rpx;line-height:1.7}
</style>
