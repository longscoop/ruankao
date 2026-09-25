<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listBases, listAgents, listKnowledgeExams } from '@/api/knowledge'
import type { KnowledgeBase, KnowledgeAgent, ExamOption } from '@/types/knowledge'
import KnowledgeLibrary from '@/components/knowledge/KnowledgeLibrary.vue'
import AgentSettings from '@/components/knowledge/AgentSettings.vue'
import AgentChat from '@/components/knowledge/AgentChat.vue'
const bases = ref<KnowledgeBase[]>([])
const agents = ref<KnowledgeAgent[]>([])
const exams = ref<ExamOption[]>([])
const tab = ref('library')
const loading = ref(false)
const error = ref('')
async function refresh() {
  loading.value = true; error.value = ''
  try { [bases.value, agents.value, exams.value] = await Promise.all([listBases(), listAgents(), listKnowledgeExams()]) }
  catch (e) { error.value = (e as Error).message || '知识库加载失败' }
  finally { loading.value = false }
}
onMounted(refresh)
</script>
<template>
  <section class="knowledge-center">
    <div class="intro"><div><h2>让学习资料成为有出处的答案</h2><p>创建知识库 → 上传并预览 → 人工发布 → 配置和启用智能体 → 问答验证</p></div><el-button :loading="loading" @click="refresh">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert title="资料仅在发布后参与问答；AI 引用仍需人工核对。当前使用中文词法检索，不支持扫描件 OCR 或向量语义检索。" type="info" :closable="false" />
    <el-tabs v-model="tab" class="tabs" v-loading="loading">
      <el-tab-pane label="知识库与资料" name="library"><KnowledgeLibrary :bases="bases" :exams="exams" :refresh="refresh" /></el-tab-pane>
      <el-tab-pane label="智能体配置" name="agents"><AgentSettings :agents="agents" :bases="bases" :refresh="refresh" /></el-tab-pane>
      <el-tab-pane label="问答调试" name="chat" lazy><AgentChat :agents="agents" /></el-tab-pane>
    </el-tabs>
  </section>
</template>
<style scoped>
.knowledge-center{padding:28px;max-width:1500px;margin:auto}.intro{display:flex;justify-content:space-between;gap:24px;align-items:center;margin-bottom:22px}.intro h2{margin:0;font-size:23px;color:#172033}.intro p{line-height:1.7;color:#667085;margin-bottom:0}.tabs{margin-top:24px}.el-alert+.el-alert{margin-top:12px}@media(max-width:700px){.knowledge-center{padding:16px}.intro{align-items:flex-start}.intro h2{font-size:19px}}
</style>
