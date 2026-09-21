<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTodayPlan } from '@/api/learning'
import { routeForTask, summarizePlan, taskLabel } from '@/lib/daily-learning'
import type { AppRequestError } from '@/lib/errors'
import type { DailyPlanDto } from '@/types/api'

const plan = ref<DailyPlanDto | null>(null)
const loading = ref(false)
const error = ref('')
const summary = computed(() => plan.value ? summarizePlan(plan.value) : null)

async function load() {
  loading.value = true
  error.value = ''
  try { plan.value = await getTodayPlan() }
  catch (e) { error.value = (e as AppRequestError).message || '计划加载失败' }
  finally { loading.value = false }
}

onShow(load)
</script>

<template>
  <view class="ruankao-page">
    <view v-if="plan" class="summary ruankao-card">
      <text class="date">{{ plan.planDate }}</text>
      <text class="title">今天安排 {{ summary?.plannedMinutes }} 分钟</text>
      <text class="muted">目标 {{ summary?.targetMinutes }} 分钟 · {{ summary?.taskCount }} 个任务</text>
    </view>
    <view v-if="plan?.tasks.length" class="tasks">
      <view v-for="(task, i) in plan.tasks" :key="task.taskId" class="task ruankao-card" @tap="uni.navigateTo({ url: routeForTask(task) })">
        <view class="index">{{ String(i + 1).padStart(2, '0') }}</view>
        <view class="body"><text class="name">{{ taskLabel(task.taskType) }}</text><text class="muted">预计 {{ task.estimatedMinutes }} 分钟<text v-if="task.priorityScore != null"> · 优先级 {{ Math.round(task.priorityScore) }}</text></text></view>
        <text class="arrow">›</text>
      </view>
    </view>
    <view v-else-if="!loading && !error" class="empty ruankao-card"><text class="empty-title">今天没有待执行任务</text><text class="muted">当学习档案和内容准备好后，计划会由服务端按规则生成。</text></view>
    <text v-if="loading" class="status">加载今日计划…</text>
    <view v-if="error" class="error"><text>{{ error }}</text><button class="retry" @tap="load">重试</button></view>
  </view>
</template>

<style scoped>
.summary { padding: 34rpx; background: linear-gradient(135deg, #3658d4, #6b82e8); color: white; }
.date { font-size: 22rpx; opacity: .78; }
.title { display: block; margin-top: 20rpx; font-size: 42rpx; font-weight: 850; }
.muted { display: block; margin-top: 8rpx; color: #7b8498; font-size: 23rpx; line-height: 1.6; }
.summary .muted { color: rgba(255,255,255,.8); }
.tasks { margin-top: 28rpx; display: flex; flex-direction: column; gap: 18rpx; }
.task { padding: 26rpx; display: flex; align-items: center; gap: 24rpx; }
.index { width: 64rpx; height: 64rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; background: #eef1ff; color: #3658d4; font-size: 22rpx; font-weight: 800; }
.body { flex: 1; }
.name { font-size: 29rpx; font-weight: 750; }
.arrow { color: #98a2b3; font-size: 42rpx; }
.empty { margin-top: 30rpx; padding: 48rpx 32rpx; text-align: center; }
.empty-title { font-size: 30rpx; font-weight: 800; }
.status { display: block; margin-top: 30rpx; color: #7b8498; font-size: 24rpx; }
.error { margin-top: 28rpx; padding: 24rpx; border-radius: 20rpx; background: #fff0f0; color: #a33535; font-size: 24rpx; }
.retry { margin-top: 14rpx; background: transparent; color: #3658d4; font-size: 24rpx; }
</style>
