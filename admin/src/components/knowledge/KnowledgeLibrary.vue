<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveBase, removeBase, listDocuments, uploadMaterial, listChunks, publishDocument, removeDocument } from '@/api/knowledge'
import { sourceLabel, validateMaterial } from '@/lib/knowledge'
import type { KnowledgeBase, ExamOption, MaterialDocument, MaterialChunk } from '@/types/knowledge'
const props = defineProps<{ bases: KnowledgeBase[]; exams: ExamOption[]; refresh: () => Promise<void> }>()
const selected = ref('')
const current = computed(() => props.bases.find(base => base.id === selected.value))
const documents = ref<MaterialDocument[]>([])
const offset = ref(0)
const busy = ref(false)
const error = ref('')
const file = ref<File>()
const fileInput = ref<HTMLInputElement>()
const editing = ref<string>()
const baseDialog = ref(false)
const form = reactive({ examId: undefined as number | undefined, name: '', description: '' })
const preview = ref<MaterialDocument>()
const previewOpen = ref(false)
const chunks = ref<MaterialChunk[]>([])
const moreChunks = ref(false)
let listVersion = 0
async function loadDocuments(nextOffset = 0) {
  const version = ++listVersion; const id = selected.value
  if (!id) { documents.value = []; offset.value = 0; return }
  const rows = await listDocuments(id, nextOffset)
  if (version === listVersion && id === selected.value) { documents.value = rows; offset.value = nextOffset }
}
async function run(work: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await work() } catch (e) { error.value = (e as Error).message || '操作失败，请重试' }
  finally { busy.value = false }
}
watch(() => props.bases, () => {
  if (!props.bases.some(base => base.id === selected.value)) {
    selected.value = props.bases[0]?.id || ''
    void loadDocuments().catch(e => { error.value = (e as Error).message })
  }
}, { immediate: true })
function changeBase() { void run(() => loadDocuments()) }
function editBase(base?: KnowledgeBase) {
  editing.value = base?.id
  Object.assign(form, { examId: base?.examId ?? props.exams[0]?.id, name: base?.name || '', description: base?.description || '' })
  baseDialog.value = true
}
async function submitBase() {
  await run(async () => {
    if (!form.examId || !form.name.trim()) throw new Error('请选择考试并填写知识库名称')
    const saved = await saveBase(editing.value, { examId: form.examId, name: form.name.trim(), description: form.description.trim() })
    await props.refresh(); selected.value = saved.id; await loadDocuments(); baseDialog.value = false
    ElMessage.success('知识库已保存')
  })
}
async function confirm(message: string): Promise<boolean> {
  try { await ElMessageBox.confirm(message, '确认操作', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); return true }
  catch { return false }
}
async function deleteBase() {
  const base = current.value
  if (!base || !await confirm(`删除知识库“${base.name}”？库内存在资料或智能体关联时，服务器将拒绝删除。`)) return
  await run(async () => { await removeBase(base.id); await props.refresh(); ElMessage.success('知识库已删除') })
}
function pickFile(event: Event) {
  file.value = undefined; error.value = ''
  const chosen = (event.target as HTMLInputElement).files?.[0]
  if (!chosen) return
  try { validateMaterial(chosen.name, chosen.size); file.value = chosen }
  catch (e) { error.value = (e as Error).message; if (fileInput.value) fileInput.value.value = '' }
}
async function upload() {
  await run(async () => {
    if (!selected.value || !file.value) throw new Error('请选择知识库和资料文件')
    validateMaterial(file.value.name, file.value.size)
    const result = await uploadMaterial(selected.value, file.value)
    file.value = undefined; if (fileInput.value) fileInput.value.value = ''
    await loadDocuments()
    ElMessage.success(result.status === 'PUBLISHED' ? '相同资料已存在，仍保持已发布状态' : '资料已保存，预览核对后才能发布')
    await openPreviewData(result)
  })
}
async function openPreviewData(document: MaterialDocument) {
  preview.value = document; previewOpen.value = true; chunks.value = []; moreChunks.value = false
  const rows = await listChunks(document.id); chunks.value = rows; moreChunks.value = rows.length === 50
}
function openPreview(document: MaterialDocument) { void run(() => openPreviewData(document)) }
async function loadMoreChunks() {
  await run(async () => {
    if (!preview.value) return
    const rows = await listChunks(preview.value.id, chunks.value.length)
    chunks.value.push(...rows); moreChunks.value = rows.length === 50
  })
}
async function togglePublication(document: MaterialDocument) {
  const publish = document.status !== 'PUBLISHED'
  if (!await confirm(publish ? '确认已检查原文、解析警告且有权使用此资料？发布后，关联智能体的所有学员均可检索。' : '撤回后该资料将不再参与新问答；已有会话保留回答时的引用快照。')) return
  await run(async () => { await publishDocument(document.id, publish); await loadDocuments(offset.value); ElMessage.success(publish ? '资料已发布' : '资料已撤回') })
}
async function deleteDocument(document: MaterialDocument) {
  if (!await confirm(`永久删除“${document.filename}”及其检索分片？已有会话的引用快照仍会保留。`)) return
  await run(async () => { await removeDocument(document.id); await loadDocuments(offset.value); ElMessage.success('资料已删除') })
}
</script>
<template>
  <div class="library">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="toolbar">
      <el-select v-model="selected" :disabled="busy" placeholder="选择知识库" class="base-select" @change="changeBase"><el-option v-for="base in bases" :key="base.id" :label="base.name" :value="base.id" /></el-select>
      <el-button :disabled="busy || !exams.length" type="primary" @click="editBase()">新建知识库</el-button>
      <el-button :disabled="busy || !current" @click="editBase(current)">编辑</el-button>
      <el-button :disabled="busy || !current" type="danger" plain @click="deleteBase">删除库</el-button>
    </div>
    <p v-if="!exams.length" class="muted">没有可用考试，请先在现有考试数据中配置并启用考试。</p>
    <el-empty v-if="!current" description="尚无知识库。新建后即可上传软考资料。" />
    <template v-else>
      <p class="muted">{{ current.description || '未填写说明' }}</p>
      <div class="upload-panel">
        <strong>上传学习资料</strong>
        <p>文本型 PDF / UTF-8 TXT / Markdown；最多 20 MiB、500 页。扫描件需先 OCR。相同文件在同一库内自动去重。</p>
        <input ref="fileInput" type="file" accept=".pdf,.txt,.md" :disabled="busy" aria-label="选择软考资料" @change="pickFile" />
        <el-button type="primary" :disabled="!file || busy" :loading="busy" @click="upload">上传并解析</el-button>
      </div>
      <el-table :data="documents" v-loading="busy" empty-text="当前页没有资料" row-key="id">
        <el-table-column prop="filename" label="资料文件" min-width="230" />
        <el-table-column prop="pageCount" label="页 / 文本单元" width="125" />
        <el-table-column prop="chunkCount" label="分片数" width="90" />
        <el-table-column label="状态" width="110"><template #default="scope"><el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'warning'">{{ scope.row.status === 'PUBLISHED' ? '已发布' : '待审核' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" min-width="230"><template #default="scope">
          <el-button link type="primary" :disabled="busy" @click="openPreview(scope.row)">预览原文</el-button>
          <el-button link :type="scope.row.status === 'PUBLISHED' ? 'warning' : 'success'" :disabled="busy" @click="togglePublication(scope.row)">{{ scope.row.status === 'PUBLISHED' ? '撤回' : '发布' }}</el-button>
          <el-button link type="danger" :disabled="busy" @click="deleteDocument(scope.row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-button :disabled="busy || offset === 0" @click="run(() => loadDocuments(Math.max(0, offset - 50)))">上一页</el-button><span>第 {{ offset / 50 + 1 }} 页</span><el-button :disabled="busy || documents.length < 50" @click="run(() => loadDocuments(offset + 50))">下一页</el-button></div>
    </template>
    <el-dialog v-model="baseDialog" :title="editing ? '编辑知识库' : '新建知识库'" width="min(560px, 94vw)" :close-on-click-modal="!busy">
      <el-form label-position="top" :disabled="busy">
        <el-form-item label="所属考试"><el-select v-model="form.examId" :disabled="Boolean(editing)"><el-option v-for="exam in exams" :key="exam.id" :label="exam.name" :value="exam.id" /></el-select></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="120" show-word-limit /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="busy" @click="baseDialog = false">取消</el-button><el-button type="primary" :loading="busy" @click="submitBase">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="previewOpen" :title="preview?.filename || '原文预览'" width="min(900px, 96vw)">
      <el-alert v-for="warning in preview?.warnings || []" :key="warning" :title="warning" type="warning" :closable="false" />
      <p class="muted">这里展示实际入库的提取文字，而非 PDF 原始版式。请核对缺字、公式、表格和扫描页。</p>
      <article v-for="chunk in chunks" :key="chunk.id" class="chunk"><strong>{{ sourceLabel({ filename: preview?.filename || '', page: chunk.page }) }} · 分片 {{ chunk.ordinal + 1 }}</strong><pre>{{ chunk.text }}</pre></article>
      <el-empty v-if="!chunks.length && !busy" description="没有可显示的分片" />
      <el-button v-if="moreChunks" :loading="busy" @click="loadMoreChunks">加载更多原文</el-button>
    </el-dialog>
  </div>
</template>
<style scoped>
.toolbar{display:flex;flex-wrap:wrap;gap:10px;margin:18px 0}.base-select{width:280px}.muted{color:#667085;line-height:1.7}.upload-panel{background:#f5f7fc;border:1px solid #e4e7ed;border-radius:12px;padding:20px;margin:18px 0}.upload-panel p{font-size:13px;color:#667085;line-height:1.7}.upload-panel input{max-width:100%;margin:8px 18px 8px 0}.pager{display:flex;gap:16px;align-items:center;justify-content:flex-end;margin-top:20px}.pager span{font-size:13px;color:#667085}.chunk{padding:18px 0;border-bottom:1px solid #e4e7ed}.chunk strong{font-size:13px;color:#52637e}.chunk pre{font-family:inherit;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;color:#172033}.el-select{max-width:100%}
</style>
