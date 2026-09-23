<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  aiSuggest, approveAll, approveItem, confirmImport, getImport, getPagePreviewUrl,
  listImports, publishKnowledge, publishLesson, publishQuestion, updateImportTitle,
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
const batchTitle = ref('')
const batchKeyword = ref('')
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
const filteredBatches = computed(() => {
  const keyword = batchKeyword.value.trim().toLowerCase()
  if (!keyword) return batches.value
  return batches.value.filter(batch =>
    (batch.title || batch.filename).toLowerCase().includes(keyword)
    || batch.status.toLowerCase().includes(keyword)
    || batch.detectedType.toLowerCase().includes(keyword),
  )
})
const completedItems = computed(() =>
  detail.value?.items.filter(item =>
    ['APPROVED', 'MATERIALIZED', 'PUBLISHED'].includes(item.status),
  ).length || 0,
)

async function refreshList() {
  batches.value = await listImports(examId.value)
}

async function selectBatch(batch: BatchSummary) {
  loading.value = true
  try {
    detail.value = await getImport(batch.batchId)
    batchTitle.value = detail.value.title || detail.value.filename
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
    ElMessage.success('解析完成：' + result.itemCount + ' 个内容项，' + result.issueCount + ' 个问题')
    await refreshList()
    const batch = batches.value.find(x => x.batchId === result.batchId)
    if (batch) await selectBatch(batch)
  } finally {
    loading.value = false
  }
}

async function saveBatchTitle() {
  if (!detail.value || !batchTitle.value.trim()) return
  await updateImportTitle(detail.value.batchId, batchTitle.value.trim())
  await refreshDetail()
  await refreshList()
  ElMessage.success('导入标题已更新')
}

async function doApproveAll() {
  if (!detail.value) return
  await approveAll(detail.value.batchId)
  await refreshDetail()
  ElMessage.success('可批准内容已批量处理')
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
  ElMessage.success('内容项已保存并重新进入审核')
}

async function doConfirm() {
  if (!detail.value) return
  const key = 'pdf-' + detail.value.batchId
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
      <el-card class="panel-card upload-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <span class="section-kicker">QUICK IMPORT</span>
              <strong>上传 PDF</strong>
            </div>
            <span class="step-badge">01</span>
          </div>
        </template>

        <el-alert
          class="compact-alert"
          type="info"
          :closable="false"
          title="解析只生成待审核内容，不会自动发布。"
        />

        <el-form class="upload-form" label-position="top">
          <el-form-item label="考试 ID">
            <el-input-number v-model="examId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="PDF 文件">
            <label class="file-picker">
              <input type="file" accept="application/pdf,.pdf" @change="onFileChange" />
              <span class="file-picker-main">{{ file?.name || '选择 PDF 文件' }}</span>
              <span class="file-picker-sub">{{ file ? '点击可重新选择' : '支持题库、讲义与知识串讲 PDF' }}</span>
            </label>
          </el-form-item>
          <el-button class="full-button" type="primary" :loading="loading" @click="doUpload">
            解析并 Dry Run
          </el-button>
        </el-form>
      </el-card>

      <el-card class="panel-card batch-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <span class="section-kicker">BATCHES</span>
              <strong>导入批次</strong>
            </div>
            <el-button text @click="refreshList">刷新</el-button>
          </div>
        </template>

        <el-input
          v-model="batchKeyword"
          class="batch-search"
          clearable
          placeholder="搜索标题、类型或状态"
        />

        <div class="batch-list">
          <button
            v-for="batch in filteredBatches"
            :key="batch.batchId"
            type="button"
            class="batch"
            :class="{ active: detail?.batchId === batch.batchId }"
            @click="selectBatch(batch)"
          >
            <div class="batch-title-row">
              <strong>{{ batch.title || batch.filename }}</strong>
              <el-tag size="small" effect="plain">{{ batch.status }}</el-tag>
            </div>
            <span>{{ batch.detectedType }} · {{ batch.pageCount }} 页</span>
          </button>

          <el-empty
            v-if="!filteredBatches.length"
            :image-size="72"
            description="暂无匹配批次"
          />
        </div>
      </el-card>
    </aside>

    <section class="content">
      <div v-if="!detail" class="empty-panel">
        <el-empty
          description="选择左侧导入批次开始审核"
          :image-size="140"
        >
          <template #description>
            <p>选择一个导入批次开始审核</p>
            <span>你可以核对原 PDF、修正结构、处理问题并逐项发布。</span>
          </template>
        </el-empty>
      </div>

      <template v-else>
        <div class="metric-grid">
          <div class="metric-card">
            <span>PDF 页数</span>
            <strong>{{ detail.pageCount }}</strong>
            <small>源文档规模</small>
          </div>
          <div class="metric-card">
            <span>内容项</span>
            <strong>{{ detail.items.length }}</strong>
            <small>{{ completedItems }} 项已通过或完成</small>
          </div>
          <div class="metric-card" :class="{ warning: openIssues.length }">
            <span>待处理问题</span>
            <strong>{{ openIssues.length }}</strong>
            <small>需人工确认</small>
          </div>
          <div class="metric-card">
            <span>批次状态</span>
            <strong class="metric-status">{{ detail.status }}</strong>
            <small>{{ detail.detectedType }}</small>
          </div>
        </div>

        <div class="detail-head">
          <div class="detail-title-block">
            <span class="section-kicker">CURRENT BATCH</span>
            <div class="title-edit">
              <el-input v-model="batchTitle" />
              <el-button @click="saveBatchTitle">保存标题</el-button>
            </div>
            <p>#{{ detail.batchId }} · {{ detail.filename }}</p>
          </div>
          <div class="actions">
            <el-button @click="doApproveAll">批准无误项</el-button>
            <el-button type="primary" @click="doConfirm">确认导入</el-button>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :xs="24" :xl="10">
            <el-card class="panel-card preview-card" shadow="never">
              <template #header>
                <div class="row-between">
                  <div class="card-title">
                    <strong>原 PDF 页</strong>
                    <span>对照源材料审核解析结果</span>
                  </div>
                  <el-select v-model="pageNumber" style="width: 126px" @change="refreshPreview">
                    <el-option
                      v-for="page in detail.pages"
                      :key="page.pageNumber"
                      :label="'第 ' + page.pageNumber + ' 页'"
                      :value="page.pageNumber"
                    />
                  </el-select>
                </div>
              </template>
              <div class="preview-canvas">
                <img v-if="previewUrl" class="preview" :src="previewUrl" />
                <el-empty v-else :image-size="80" description="暂无页面预览图" />
              </div>
              <details class="source-details">
                <summary>查看识别文本</summary>
                <pre class="source-text">{{ detail.pages.find(x => x.pageNumber === pageNumber)?.textContent }}</pre>
              </details>
            </el-card>
          </el-col>

          <el-col :xs="24" :xl="14">
            <el-card class="panel-card issue-card" shadow="never">
              <template #header>
                <div class="row-between">
                  <div class="card-title">
                    <strong>解析问题</strong>
                    <span>优先清理未解决问题，再确认批次</span>
                  </div>
                  <el-tag :type="openIssues.length ? 'warning' : 'success'" effect="light">
                    {{ openIssues.length }} 未解决
                  </el-tag>
                </div>
              </template>
              <el-table :data="detail.issues" size="small" stripe>
                <el-table-column prop="severity" label="级别" width="82" />
                <el-table-column prop="code" label="代码" min-width="150" />
                <el-table-column prop="message" label="说明" min-width="220" />
                <el-table-column prop="sourcePage" label="页" width="56" />
                <el-table-column label="操作" width="88" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.status === 'OPEN'"
                      link
                      type="primary"
                      @click="resolve(row.id)"
                    >
                      解决
                    </el-button>
                    <el-tag v-else size="small" type="success" effect="plain">已解决</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="panel-card items-card" shadow="never">
          <template #header>
            <div class="row-between">
              <div class="card-title">
                <strong>内容项</strong>
                <span>审核结构化结果并显式发布到学习内容</span>
              </div>
              <span class="table-count">{{ detail.items.length }} 项</span>
            </div>
          </template>
          <el-table :data="detail.items" stripe>
            <el-table-column prop="itemType" label="类型" width="110">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.itemType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="来源页" width="96">
              <template #default="{ row }">
                {{ row.sourcePageStart }}
                <span v-if="row.sourcePageEnd !== row.sourcePageStart">-{{ row.sourcePageEnd }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="126" />
            <el-table-column prop="targetId" label="目标 ID" width="88" />
            <el-table-column label="审核 / 发布" width="320" fixed="right">
              <template #default="{ row }">
                <el-button link @click="openEdit(row)" :disabled="['MATERIALIZED', 'PUBLISHED'].includes(row.status)">编辑</el-button>
                <el-button link type="warning" @click="showAiSuggestion(row)">AI 建议</el-button>
                <el-button link type="success" @click="setItemStatus(row, true)" :disabled="['MATERIALIZED', 'PUBLISHED'].includes(row.status)">批准</el-button>
                <el-button link type="danger" @click="setItemStatus(row, false)" :disabled="['MATERIALIZED', 'PUBLISHED'].includes(row.status)">驳回</el-button>
                <el-button
                  link
                  type="primary"
                  @click="openPublish(row)"
                  :disabled="row.itemType === 'LESSON' ? row.status !== 'MATERIALIZED' : row.status !== 'APPROVED'"
                >
                  发布
                </el-button>
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
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存并重新审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiDialog" title="AI 结构审核建议" width="680px">
      <el-alert type="warning" :closable="false" title="AI 只提供结构建议，不会覆盖原始内容或自动发布。" />
      <pre class="ai-suggestion">{{ aiSuggestion }}</pre>
    </el-dialog>

    <el-dialog v-model="publishDialog" :title="'发布 ' + (selectedItem?.itemType || '')" width="560px">
      <el-form v-if="selectedItem" label-position="top">
        <template v-if="selectedItem.itemType === 'LESSON'">
          <el-form-item label="关联知识点 ID（逗号分隔）"><el-input v-model="publishForm.knowledgeIds" /></el-form-item>
        </template>
        <template v-else-if="selectedItem.itemType === 'KNOWLEDGE'">
          <el-form-item label="知识点 Code"><el-input v-model="publishForm.code" /></el-form-item>
          <el-form-item label="父知识点 ID（可空）"><el-input v-model="publishForm.parentId" /></el-form-item>
          <el-form-item label="重要度 1-5"><el-input-number v-model="publishForm.importance" :min="1" :max="5" /></el-form-item>
          <el-form-item label="考试频率 0-100"><el-input-number v-model="publishForm.examFrequency" :min="0" :max="100" /></el-form-item>
          <el-form-item label="预计学习分钟"><el-input-number v-model="publishForm.estimatedMinutes" :min="1" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="主知识点 ID"><el-input v-model="publishForm.knowledgeIds" /></el-form-item>
          <el-form-item label="难度">
            <el-select v-model="publishForm.difficulty">
              <el-option label="EASY" value="EASY" />
              <el-option label="MEDIUM" value="MEDIUM" />
              <el-option label="HARD" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="题目来源">
            <el-select v-model="publishForm.source">
              <el-option label="真题 REAL_EXAM" value="REAL_EXAM" />
              <el-option label="章节题 CHAPTER" value="CHAPTER" />
              <el-option label="模拟题 SIMULATION" value="SIMULATION" />
              <el-option label="人工资料 MANUAL" value="MANUAL" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>
