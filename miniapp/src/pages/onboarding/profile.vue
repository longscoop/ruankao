<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getExamProfile, listExams, updateExamProfile } from '@/api/onboarding'
import { validateExamProfile } from '@/lib/profile'
import type { AppRequestError } from '@/lib/errors'
import type { ExamDto, FoundationLevel } from '@/types/api'

const exams = ref<ExamDto[]>([])
const examId = ref(0)
const examDate = ref('')
const dailyTargetMinutes = ref(30)
const foundationLevel = ref<FoundationLevel>('SOME')
const loading = ref(false)
const error = ref('')
const targets = [15, 30, 60, 90]
const levels: Array<{ value: FoundationLevel; label: string }> = [
  { value: 'ZERO', label: '零基础' },
  { value: 'SOME', label: '有一些基础' },
  { value: 'REVIEWING', label: '正在复习/冲刺' },
]

const selectedExamName = computed(() => exams.value.find((item) => item.id === examId.value)?.name || '请选择考试')

onMounted(async () => {
  try {
    exams.value = await listExams()
    if (exams.value.length === 1) examId.value = exams.value[0].id
  } catch (e) {
    error.value = (e as AppRequestError).message || '考试目录暂不可用'
    return
  }

  try {
    const profile = await getExamProfile()
    examId.value = profile.examId
    examDate.value = profile.examDate
    dailyTargetMinutes.value = profile.dailyTargetMinutes
    foundationLevel.value = profile.foundationLevel
  } catch (e) {
    if ((e as AppRequestError).code !== 'NOT_FOUND') {
      error.value = (e as AppRequestError).message || '学习设置加载失败'
    }
  }
})

function onExamChange(event: { detail: { value: number } }) {
  examId.value = exams.value[event.detail.value]?.id || 0
}

function onDateChange(event: { detail: { value: string } }) {
  examDate.value = event.detail.value
}

async function save() {
  error.value = ''
  const today = new Date().toISOString().slice(0, 10)
  const errors = validateExamProfile({ examId: examId.value, examDate: examDate.value, dailyTargetMinutes: dailyTargetMinutes.value }, today)
  if (errors.length) {
    error.value = errors[0]
    return
  }
  loading.value = true
  try {
    await updateExamProfile({ examId: examId.value, examDate: examDate.value, dailyTargetMinutes: dailyTargetMinutes.value, foundationLevel: foundationLevel.value })
    uni.redirectTo({ url: `/pages/assessment/intro?examId=${examId.value}` })
  } catch (e) {
    error.value = (e as AppRequestError).message || '保存失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="ruankao-page">
    <text class="step">1 / 2</text>
    <text class="title">先定一个能坚持的学习节奏</text>
    <view class="form ruankao-card">
      <view class="field">
        <text class="label">目标考试</text>
        <picker :range="exams" range-key="name" @change="onExamChange">
          <view class="picker">{{ selectedExamName }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="label">考试日期</text>
        <picker mode="date" :value="examDate" @change="onDateChange">
          <view class="picker">{{ examDate || '选择日期' }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="label">每天投入多久</text>
        <view class="chips"><button v-for="item in targets" :key="item" class="chip" :class="{ active: dailyTargetMinutes === item }" @tap="dailyTargetMinutes = item">{{ item }} 分钟</button></view>
      </view>
      <view class="field">
        <text class="label">目前基础</text>
        <view class="chips"><button v-for="item in levels" :key="item.value" class="chip" :class="{ active: foundationLevel === item.value }" @tap="foundationLevel = item.value">{{ item.label }}</button></view>
      </view>
    </view>
    <text v-if="error" class="error">{{ error }}</text>
    <button class="primary" :loading="loading" @tap="save">保存并继续</button>
  </view>
</template>

<style scoped>
.step { color: #3658d4; font-size: 24rpx; font-weight: 700; }
.title { display: block; margin: 20rpx 0 36rpx; font-size: 44rpx; line-height: 1.35; font-weight: 800; }
.form { padding: 34rpx; }
.field + .field { margin-top: 34rpx; }
.label { display: block; margin-bottom: 14rpx; color: #475467; font-size: 24rpx; font-weight: 700; }
.picker { padding: 24rpx; border-radius: 18rpx; background: #f7f8fc; font-size: 28rpx; }
.chips { display: flex; flex-wrap: wrap; gap: 16rpx; }
.chip { margin: 0; padding: 0 22rpx; border-radius: 999rpx; background: #f2f4f7; color: #475467; font-size: 24rpx; }
.chip.active { background: #e8edff; color: #3658d4; font-weight: 700; }
.error { display: block; margin-top: 24rpx; color: #c43d3d; font-size: 24rpx; }
.primary { margin-top: 36rpx; border-radius: 24rpx; background: #3658d4; color: #fff; font-weight: 700; }
</style>
