<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  aiSuggest, approveAll, approveItem, confirmImport, getImport, getPagePreviewUrl,
  listImports, publishKnowledge, publishLesson, publishQuestion,
  rejectItem, resolveIssue, updateItem, uploadPdf,
} from '@/api/imports'
import { parseKnowledgeIds, parseQuestionLinks } from '@/lib/import-review'
import type { BatchSummary, ImportDetail, ImportItem } from '@/types/import'

const examId = ref<number>()
const file = ref<File>()
const batches = ref<BatchSummary[]>([])
const detail = ref<ImportDetail>()
const loading = ref(false)
const pageNumber = ref(1)
const previewUrl = ref('')
const selectedItem = ref<ImportItem>()
const editDialog = ref(false)
const publishDialog = ref(false)
const aiDialog = ref(false)
const aiSuggestion = ref('')
const editForm = reactive({ title: '', contentJson: '' })
const publishForm = reactive({
  knowledgeIds: '',
  code: '',
  parentId: '',
  importance: 3,
  examFrequency: 0,
  estimatedMinutes: 10,
  sortOrder: 0,
  difficulty: 'MEDIUM' as 'EASY' | 'MEDIUM' | 'HARD',
  source: 'MANUAL' as 'REAL_EXAM' | 'CHAPTER' | 'SIMULATION' | 'MANUAL',
})

const openIssues = computed(() => detail.value?.issues.filter(x => x.status === 'OPEN') || [])

async function refreshList() {
  batches.value = await listImports(examId.value)
}

async function selectBatch(batch: BatchSummary) {
  loading.value = true
  try {
    detail.value = await getImport(batch.batchId)
    pageNumber.value = detail.value.pages[0]?.pageNumber || 1
    await refreshPreview()
  } finally {
    loading.value = false
  }
}

async function refreshDetail() {
  if (!detail.value) return
  detail.value = await getImport(detail.value.batchId)
}

async function refreshPreview() {
  if (!detail.value || !pageNumber.value) return
  try {
    previewUrl.value = (await getPagePreviewUrl(detail.value.batchId, pageNumber.value)).url
  } catch {
    previewUrl.value = ''
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  file.value = input.files?.[0]
}

async function doUpload() {
  if (!examId.value || !file.value) {
    ElMessage.warning('请先填写考试 ID 并选择 PDF')
    return
  }
  loading.value = true
  try {
    const result = await uploadPdf(examId.value, file.value)
    ElMessage.success(`解析完成：${result.itemCount} 个内容项，${result.issueCount} 个问题`)
    await refreshList()
    const batch = batches.value.find(x => x.batchId === result.batchId)
    if (batch) await selectBatch(batch)
  } finally {
    loading.value = false
  }
}

async function doApproveAll() {
  if (!detail.value) return
  await approveAll(detail.value.batchId)
  await refreshDetail()
}

async function setItemStatus(item: ImportItem, approve: boolean) {
  if (!detail.value) return
  if (approve) await approveItem(detail.value.batchId, item.id)
  else await rejectItem(detail.value.batchId, item.id)
  await refreshDetail()
}

function openEdit(item: ImportItem) {
  selectedItem.value = item
  editForm.title = item.title || ''
  editForm.contentJson = JSON.stringify(JSON.parse(item.contentJson), null, 2)
  editDialog.value = true
}

async function saveEdit() {
  if (!detail.value || !selectedItem.value) return
  await updateItem(detail.value.batchId, selectedItem.value.id, editForm.title, editForm.contentJson)
  editDialog.value = false
  await refreshDetail()
}

async function doConfirm() {
  if (!detail.value) return
  const key = `pdf-${detail.value.batchId}`
  await confirmImport(detail.value.batchId, key)
  ElMessage.success('已确认导入；讲义已进入 REVIEW，仍需显式发布')
  await refreshDetail()
}

function openPublish(item: ImportItem) {
  selectedItem.value = item
  publishForm.knowledgeIds = ''
  publishForm.code = ''
  publishForm.parentId = ''
  publishForm.importance = 3
  publishForm.examFrequency = 0
  publishForm.estimatedMinutes = 10
  publishForm.sortOrder = 0
  publishForm.difficulty = 'MEDIUM'
  publishForm.source = 'MANUAL'
  publishDialog.value = true
}

async function doPublish() {
  if (!detail.value || !selectedItem.value) return
  const item = selectedItem.value
  if (item.itemType === 'LESSON') {
    const ids = parseKnowledgeIds(publishForm.knowledgeIds)
    if (!ids.length) return ElMessage.warning('讲义发布前必须关联至少一个知识点')
    await publishLesson(detail.value.batchId, item.id, ids)
  } else if (item.itemType === 'KNOWLEDGE') {
    if (!publishForm.code.trim()) return ElMessage.warning('知识点 code 必填')
    await publishKnowledge(detail.value.batchId, item.id, {
      code: publishForm.code.trim(),
      parentId: publishForm.parentId ? Number(publishForm.parentId) : null,
      importance: publishForm.importance,
      examFrequency: publishForm.examFrequency,
      estimatedMinutes: publishForm.estimatedMinutes,
      sortOrder: publishForm.sortOrder,
    })
  } else {
    const links = parseQuestionLinks(publishForm.knowledgeIds)
    if (!links.length) return ElMessage.warning('当前管理页要求先选择一个主知识点；多知识点权重可通过 JSON/API 精细设置')
    await publishQuestion(detail.value.batchId, item.id, {
      difficulty: publishForm.difficulty,
      source: publishForm.source,
      knowledgeLinks: links,
    })
  }
  publishDialog.value = false
  ElMessage.success('发布成功')
  await refreshDetail()
  await refreshList()
}

async function showAiSuggestion(item: ImportItem) {
  if (!detail.value) return
  selectedItem.value = item
  aiSuggestion.value = 'AI 正在分析结构…'
  aiDialog.value = true
  try {
    aiSuggestion.value = (await aiSuggest(detail.value.batchId, item.id)).content
  } catch (error) {
    aiSuggestion.value = error instanceof Error ? error.message : 'AI 建议获取失败'
  }
}

async function resolve(issueId: number) {
  if (!detail.value) return
  await ElMessageBox.confirm('确认已人工核对并解决这个解析问题？', '解决问题')
  await resolveIssue(detail.value.batchId, issueId)
  await refreshDetail()
}

onMounted(refreshList)
</script>

<template>
  <main class="workspace">
    <aside class="side">
      <el-card>
        <template #header>上传 PDF</template>
        <el-form label-position="top">
          <el-form-item label="考试 ID">
            <el-input-number v-model="examId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="PDF 文件">
            <input type="file" accept="application/pdf,.pdf" @change="onFileChange" />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="doUpload">解析并 Dry Run</el-button>
        </el-form>
      </el-card>

      <el-card class="batch-card">
        <template #header>
          <div class="row-between"><span>导入批次</span><el-button text @click="refreshList">刷新</el-button></div>
        </template>
        <div v-for="batch in batches" :key="batch.batchId" class="batch" @click="selectBatch(batch)">
          <strong>{{ batch.title || batch.filename }}</strong>
          <span>{{ batch.detectedType }} · {{ batch.status }}</span>
          <span>{{ batch.pageCount }} 页</span>
        </div>
      </el-card>
    </aside>

    <section class="content">
      <el-empty v-if="!detail" description="选择一个导入批次开始审核" />
      <template v-else>
        <div class="detail-head">
          <div>
            <h2>{{ detail.title || detail.filename }}</h2>
            <p>{{ detail.detectedType }} · {{ detail.status }} · {{ detail.pageCount }} 页</p>
          </div>
          <div class="actions">
            <el-button @click="doApproveAll">批准无误项</el-button>
            <el-button type="primary" @click="doConfirm">确认导入</el-button>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :span="10">
            <el-card class="preview-card">
              <template #header>
                <div class="row-between">
                  <span>原 PDF 页</span>
                  <el-select v-model="pageNumber" style="width:120px" @change="refreshPreview">
                    <el-option v-for="page in detail.pages" :key="page.pageNumber" :label="`第 ${page.pageNumber} 页`" :value="page.pageNumber" />
                  </el-select>
                </div>
              </template>
              <img v-if="previewUrl" class="preview" :src="previewUrl" />
              <pre class="source-text">{{ detail.pages.find(x=>x.pageNumber===pageNumber)?.textContent }}</pre>
            </el-card>
          </el-col>

          <el-col :span="14">
            <el-card>
              <template #header>解析问题（{{ openIssues.length }} 未解决）</template>
              <el-table :data="detail.issues" size="small">
                <el-table-column prop="severity" label="级别" width="90" />
                <el-table-column prop="code" label="代码" width="180" />
                <el-table-column prop="message" label="说明" />
                <el-table-column prop="sourcePage" label="页" width="60" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button v-if="row.status==='OPEN'" link type="primary" @click="resolve(row.id)">解决</el-button>
                    <span v-else>已解决</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="items-card">
          <template #header>内容项</template>
          <el-table :data="detail.items">
            <el-table-column prop="itemType" label="类型" width="110" />
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column label="来源页" width="100">
              <template #default="{ row }">{{ row.sourcePageStart }}<span v-if="row.sourcePageEnd!==row.sourcePageStart">-{{ row.sourcePageEnd }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="targetId" label="目标 ID" width="90" />
            <el-table-column label="审核/发布" width="310">
              <template #default="{ row }">
                <el-button link @click="openEdit(row)" :disabled="['MATERIALIZED','PUBLISHED'].includes(row.status)">编辑</el-button>
                <el-button link type="warning" @click="showAiSuggestion(row)">AI建议</el-button>
                <el-button link type="success" @click="setItemStatus(row,true)" :disabled="['MATERIALIZED','PUBLISHED'].includes(row.status)">批准</el-button>
                <el-button link type="danger" @click="setItemStatus(row,false)" :disabled="['MATERIALIZED','PUBLISHED'].includes(row.status)">驳回</el-button>
                <el-button link type="primary" @click="openPublish(row)" :disabled="row.itemType==='LESSON' ? row.status!=='MATERIALIZED' : row.status!=='APPROVED'">发布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </section>

    <el-dialog v-model="editDialog" title="编辑解析结果" width="720px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="结构化 JSON"><el-input v-model="editForm.contentJson" type="textarea" :rows="18" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editDialog=false">取消</el-button><el-button type="primary" @click="saveEdit">保存并重新审核</el-button></template>
    </el-dialog>

    <el-dialog v-model="aiDialog" title="AI 结构审核建议" width="680px">
      <el-alert type="warning" :closable="false" title="AI 只提供结构建议，不会覆盖原始内容或自动发布。" />
      <pre class="ai-suggestion">{{ aiSuggestion }}</pre>
    </el-dialog>

    <el-dialog v-model="publishDialog" :title="`发布 ${selectedItem?.itemType || ''}`" width="560px">
      <el-form v-if="selectedItem" label-position="top">
        <template v-if="selectedItem.itemType==='LESSON'">
          <el-form-item label="关联知识点 ID（逗号分隔）"><el-input v-model="publishForm.knowledgeIds" /></el-form-item>
        </template>
        <template v-else-if="selectedItem.itemType==='KNOWLEDGE'">
          <el-form-item label="知识点 Code"><el-input v-model="publishForm.code" /></el-form-item>
          <el-form-item label="父知识点 ID（可空）"><el-input v-model="publishForm.parentId" /></el-form-item>
          <el-form-item label="重要度 1-5"><el-input-number v-model="publishForm.importance" :min="1" :max="5" /></el-form-item>
          <el-form-item label="考试频率 0-100"><el-input-number v-model="publishForm.examFrequency" :min="0" :max="100" /></el-form-item>
          <el-form-item label="预计学习分钟"><el-input-number v-model="publishForm.estimatedMinutes" :min="1" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="主知识点 ID"><el-input v-model="publishForm.knowledgeIds" /></el-form-item>
          <el-form-item label="难度">
            <el-select v-model="publishForm.difficulty"><el-option label="EASY" value="EASY"/><el-option label="MEDIUM" value="MEDIUM"/><el-option label="HARD" value="HARD"/></el-select>
          </el-form-item>
          <el-form-item label="题目来源">
            <el-select v-model="publishForm.source"><el-option label="真题 REAL_EXAM" value="REAL_EXAM"/><el-option label="章节题 CHAPTER" value="CHAPTER"/><el-option label="模拟题 SIMULATION" value="SIMULATION"/><el-option label="人工资料 MANUAL" value="MANUAL"/></el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer><el-button @click="publishDialog=false">取消</el-button><el-button type="primary" @click="doPublish">确认发布</el-button></template>
    </el-dialog>
  </main>
</template>
