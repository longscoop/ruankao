<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { finishAssessment, listAssessmentQuestions, submitAssessmentAnswer } from '@/api/assessment'
import { assessmentProgress, canSubmitAssessmentAnswer } from '@/lib/assessment'
import type { AppRequestError } from '@/lib/errors'
import type { AnswerConfidence, AssessmentQuestionDto, QuestionOptionDto } from '@/types/api'

const sessionId = ref('')
const questions = ref<Array<AssessmentQuestionDto & { options?: QuestionOptionDto[] }>>([])
const index = ref(0)
const answer = ref('')
const confidence = ref<AnswerConfidence>('UNCERTAIN')
const error = ref('')
const loading = ref(false)
const startedAt = ref(Date.now())
const current = computed(() => questions.value[index.value])
const progress = computed(() => assessmentProgress(index.value, questions.value.length || 1))

onLoad(async (query) => {
  sessionId.value = String(query?.sessionId || '')
  try {
    questions.value = await listAssessmentQuestions(sessionId.value)
  } catch (e) {
    error.value = (e as AppRequestError).message || '题目加载失败'
  }
})

function chooseOption(key: string) {
  answer.value = key
}

async function next() {
  const question = current.value
  if (!question || !canSubmitAssessmentAnswer(question.id, answer.value)) {
    error.value = '请先填写答案'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await submitAssessmentAnswer(sessionId.value, {
      questionId: question.id,
      answer: answer.value.trim(),
      durationSeconds: Math.max(0, Math.round((Date.now() - startedAt.value) / 1000)),
      confidence: confidence.value,
    })
    if (index.value < questions.value.length - 1) {
      index.value += 1
      answer.value = ''
      confidence.value = 'UNCERTAIN'
      startedAt.value = Date.now()
      return
    }
    const result = await finishAssessment(sessionId.value)
    uni.redirectTo({ url: `/pages/assessment/result?total=${result.totalQuestions}&correct=${result.correctQuestions}` })
  } catch (e) {
    error.value = (e as AppRequestError).message || '提交失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="ruankao-page">
    <view class="progress-row"><text>{{ progress.current }} / {{ progress.total }}</text><text>{{ progress.percent }}%</text></view>
    <view class="bar"><view class="bar-inner" :style="{ width: progress.percent + '%' }" /></view>
    <view v-if="current" class="question ruankao-card">
      <view class="meta"><text>{{ current.type }}</text><text>{{ current.difficulty }}</text></view>
      <text class="content">{{ current.content }}</text>
      <view v-if="current.options?.length" class="options">
        <button v-for="item in current.options" :key="item.key" class="option" :class="{ active: answer === item.key }" @tap="chooseOption(item.key)">{{ item.key }}. {{ item.text }}</button>
      </view>
      <textarea v-else v-model="answer" class="answer" placeholder="当前后端题目响应尚未返回选项，可先输入答案；选项字段接入后会自动切换为选择题交互。" />
      <text class="label">这题把握如何？</text>
      <view class="confidence">
        <button v-for="item in [{v:'GUESS',t:'蒙的'},{v:'UNCERTAIN',t:'不确定'},{v:'CONFIDENT',t:'确定'}]" :key="item.v" class="chip" :class="{ active: confidence === item.v }" @tap="confidence = item.v as AnswerConfidence">{{ item.t }}</button>
      </view>
    </view>
    <text v-else-if="!error" class="empty">暂无可作答题目</text>
    <text v-if="error" class="error">{{ error }}</text>
    <button class="primary" :loading="loading" :disabled="!current" @tap="next">{{ index === questions.length - 1 ? '提交摸底' : '下一题' }}</button>
  </view>
</template>

<style scoped>
.progress-row { display: flex; justify-content: space-between; color: #667085; font-size: 24rpx; }
.bar { height: 10rpx; margin: 14rpx 0 30rpx; overflow: hidden; border-radius: 999rpx; background: #e8ebf2; }
.bar-inner { height: 100%; background: #3658d4; }
.question { padding: 34rpx; }
.meta { display: flex; justify-content: space-between; color: #667085; font-size: 22rpx; }
.content { display: block; margin-top: 26rpx; font-size: 32rpx; line-height: 1.65; font-weight: 650; }
.options { margin-top: 30rpx; display: flex; flex-direction: column; gap: 16rpx; }
.option { width: 100%; margin: 0; padding: 22rpx; text-align: left; border-radius: 20rpx; background: #f7f8fc; color: #344054; font-size: 26rpx; }
.option.active { background: #e8edff; color: #2946b8; }
.answer { width: 100%; box-sizing: border-box; min-height: 180rpx; margin-top: 30rpx; padding: 22rpx; border-radius: 20rpx; background: #f7f8fc; font-size: 26rpx; }
.label { display: block; margin: 32rpx 0 14rpx; color: #667085; font-size: 24rpx; }
.confidence { display: flex; gap: 14rpx; }
.chip { flex: 1; margin: 0; border-radius: 999rpx; background: #f2f4f7; color: #667085; font-size: 24rpx; }
.chip.active { background: #e8edff; color: #3658d4; }
.primary { margin-top: 34rpx; border-radius: 24rpx; background: #3658d4; color: #fff; font-weight: 700; }
.error, .empty { display: block; margin: 28rpx 0; color: #c43d3d; font-size: 24rpx; }
.empty { color: #98a2b3; }
</style>
