<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveAgent, removeAgent } from '@/api/knowledge'
import type { KnowledgeAgent, KnowledgeBase, AgentInput } from '@/types/knowledge'
const props = defineProps<{ agents: KnowledgeAgent[]; bases: KnowledgeBase[]; refresh: () => Promise<void> }>()
const busy = ref(false)
const error = ref('')
const dialog = ref(false)
const editing = ref<string>()
const form = reactive<AgentInput>({ name: '', description: '', instructions: '', baseIds: [], enabled: false })
function edit(agent?: KnowledgeAgent) {
  editing.value = agent?.id
  Object.assign(form, { name: agent?.name || '', description: agent?.description || '', instructions: agent?.instructions || '', baseIds: [...(agent?.baseIds || [])], enabled: agent?.enabled || false })
  dialog.value = true
}
async function run(work: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await work() } catch (e) { error.value = (e as Error).message || '智能体操作失败' }
  finally { busy.value = false }
}
async function save() {
  await run(async () => {
    if (!form.name.trim() || !form.baseIds.length) throw new Error('请填写名称并关联至少一个知识库')
    if (new Set(form.baseIds.map(id => props.bases.find(base => base.id === id)?.examId)).size !== 1) throw new Error('一个智能体只能关联同一考试的知识库')
    await saveAgent(editing.value, { ...form, name: form.name.trim(), baseIds: [...form.baseIds] })
    dialog.value = false; await props.refresh(); ElMessage.success(editing.value ? '智能体已保存' : '智能体已创建，启用后才对学员可见')
  })
}
async function toggle(agent: KnowledgeAgent) {
  try { await ElMessageBox.confirm(agent.enabled ? '停用后不能继续发起新问答，已有历史仍保留。' : '启用后学员可以检索该智能体关联知识库中的已发布资料。确认启用？', '智能体状态', { confirmButtonText: '确认', cancelButtonText: '取消' }) }
  catch { return }
  await run(async () => { await saveAgent(agent.id, { ...agent, enabled: !agent.enabled }); await props.refresh(); ElMessage.success(agent.enabled ? '已停用' : '已启用') })
}
async function remove(agent: KnowledgeAgent) {
  try { await ElMessageBox.confirm(`删除“${agent.name}”？已有会话时请改为停用，服务器将保留会话历史。`, '删除智能体', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) }
  catch { return }
  await run(async () => { await removeAgent(agent.id); await props.refresh(); ElMessage.success('智能体已删除') })
}
function baseNames(agent: KnowledgeAgent) { return agent.baseIds.map(id => props.bases.find(base => base.id === id)?.name || '已不可用的知识库').join('、') }
</script>
<template>
  <div class="agents">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="toolbar"><el-button type="primary" :disabled="busy || !bases.length" @click="edit()">新建智能体</el-button><span>使用后端统一配置的模型，不在浏览器保存模型密钥。</span></div>
    <el-empty v-if="!agents.length" description="暂无智能体。先创建知识库，再配置学习助教。" />
    <div class="cards">
      <article v-for="agent in agents" :key="agent.id" class="agent-card">
        <div class="heading"><h3>{{ agent.name }}</h3><el-tag :type="agent.enabled ? 'success' : 'info'">{{ agent.enabled ? '已启用' : '已停用' }}</el-tag></div>
        <p>{{ agent.description || '未填写说明' }}</p><p class="bases">知识库：{{ baseNames(agent) }}</p>
        <div><el-button :disabled="busy" @click="edit(agent)">编辑</el-button><el-button :disabled="busy" :type="agent.enabled ? 'warning' : 'success'" plain @click="toggle(agent)">{{ agent.enabled ? '停用' : '启用' }}</el-button><el-button :disabled="busy" type="danger" text @click="remove(agent)">删除</el-button></div>
      </article>
    </div>
    <el-dialog v-model="dialog" :title="editing ? '编辑智能体' : '新建智能体'" width="min(620px, 95vw)" :close-on-click-modal="!busy">
      <el-form label-position="top" :disabled="busy">
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="120" /></el-form-item>
        <el-form-item label="学员可见说明"><el-input v-model="form.description" type="textarea" :rows="2" maxlength="1000" /></el-form-item>
        <el-form-item label="关联知识库（同一考试，最多 10 个）"><el-select v-model="form.baseIds" multiple :multiple-limit="10" style="width:100%"><el-option v-for="base in bases" :key="base.id" :label="base.name" :value="base.id" /></el-select></el-form-item>
        <el-form-item label="辅导风格指令"><el-input v-model="form.instructions" type="textarea" :rows="4" maxlength="2000" placeholder="例如：先说明概念，再给出备考易错点。此处不能覆盖资料引用和权限规则。" /></el-form-item>
        <el-form-item v-if="editing" label="学员入口"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <el-alert title="无资料命中时明确提示不足；不编造依据，不修改标准答案或掌握度。" type="info" :closable="false" />
      <template #footer><el-button :disabled="busy" @click="dialog = false">取消</el-button><el-button :loading="busy" type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
.toolbar{display:flex;align-items:center;flex-wrap:wrap;gap:18px;margin:18px 0}.toolbar span{color:#667085;font-size:13px}.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(290px,1fr));gap:18px}.agent-card{padding:22px;border:1px solid #e4e7ed;border-radius:12px;background:white}.heading{display:flex;gap:12px;justify-content:space-between;align-items:center}.heading h3{margin:0;color:#172033;font-size:18px}.agent-card p{color:#667085;line-height:1.7;overflow-wrap:anywhere}.agent-card .bases{font-size:13px}
</style>
