<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTodayPlan } from '@/api/learning'
import { routeForTask, summarizePlan, taskLabel } from '@/lib/daily-learning'
import type { AppRequestError } from '@/lib/errors'
import { useSessionStore } from '@/stores/session'
import type { DailyPlanDto } from '@/types/api'

const plan = ref<DailyPlanDto | null>(null)
const loading = ref(false)
const error = ref('')
const session = useSessionStore()
const summary = computed(() => plan.value ? summarizePlan(plan.value) : null)

async function load() {
  if (!session.restored) session.restore()
  if (!session.token) return
  loading.value = true
  error.value = ''
  try {
    plan.value = await getTodayPlan()
  } catch (e) {
    error.value = (e as AppRequestError).message || '今日计划加载失败'
  } finally {
    loading.value = false
  }
}

onShow(load)
</script>

<template>
  <view class="ruankao-page home">
    <view class="topline"><view><text class="eyebrow">TODAY</text><text class="title">今天，继续向证书靠近一点</text></view><view class="avatar" @tap="uni.navigateTo({ url: '/pages/mine/index' })">我</view></view>

    <view v-if="!session.token" class="ruankao-card setup-card">
      <text class="card-title">先完成学习设置</text>
      <text class="muted">登录、选择目标考试和每日学习时长后，服务端会生成你的今日计划。</text>
      <button class="primary small" @tap="uni.navigateTo({ url: '/pages/onboarding/welcome' })">开始设置</button>
    </view>

    <view v-else-if="plan" class="ruankao-card focus-card" @tap="uni.navigateTo({ url: '/pages/learning/today' })">
      <view class="focus-head"><text>今日学习</text><text class="date">{{ plan.planDate }}</text></view>
      <view class="minutes"><text class="big">{{ summary?.plannedMinutes }}</text><text> / {{ summary?.targetMinutes }} 分钟</text></view>
      <text class="muted">{{ summary?.taskCount }} 个学习任务 · 这里只展示服务端计划，不推测完成度</text>
    </view>

    <view v-if="plan?.tasks.length" class="section">
      <view class="section-head"><text class="section-title">先做这一件</text><text class="link" @tap="uni.navigateTo({ url: '/pages/learning/today' })">全部任务</text></view>
      <view class="task ruankao-card" @tap="uni.navigateTo({ url: routeForTask(plan.tasks[0]) })">
        <view><text class="task-type">{{ taskLabel(plan.tasks[0].taskType) }}</text><text class="muted block">预计 {{ plan.tasks[0].estimatedMinutes }} 分钟</text></view>
        <text class="arrow">→</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">学习入口</text>
      <view class="grid">
        <view class="entry" @tap="uni.navigateTo({ url: '/pages/course/index' })"><text class="entry-icon">▶</text><text>课程视频</text></view>
        <view class="entry" @tap="uni.navigateTo({ url: '/pages/practice/index' })"><text class="entry-icon">✓</text><text>题库练习</text></view>
        <view class="entry" @tap="uni.navigateTo({ url: '/pages/practice/wrong' })"><text class="entry-icon">↺</text><text>错题复习</text></view>
        <view class="entry" @tap="uni.navigateTo({ url: '/pages/ai/index' })"><text class="entry-icon">✦</text><text>AI 助教</text></view>
      </view>
    </view>

    <text v-if="loading" class="status">正在生成今天的学习安排…</text>
    <view v-if="error" class="error"><text>{{ error }}</text><button class="retry" @tap="load">重试</button></view>
  </view>
</template>

<style scoped>
.home { padding-bottom: 80rpx; }
.topline, .section-head, .task, .focus-head { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { display: block; color: #3658d4; font-size: 22rpx; font-weight: 800; letter-spacing: 4rpx; }
.title { display: block; max-width: 560rpx; margin-top: 12rpx; font-size: 40rpx; line-height: 1.35; font-weight: 850; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 24rpx; display: flex; align-items: center; justify-content: center; background: #172033; color: white; font-size: 22rpx; font-weight: 800; }
.setup-card, .focus-card { margin-top: 36rpx; padding: 32rpx; }
.card-title, .section-title { font-size: 30rpx; font-weight: 800; }
.muted { color: #7b8498; font-size: 24rpx; line-height: 1.6; }
.block { display: block; margin-top: 8rpx; }
.primary { border-radius: 22rpx; background: #3658d4; color: white; font-weight: 700; }
.small { margin: 26rpx 0 0; }
.focus-card { background: #172033; color: #fff; }
.focus-card .muted { color: #aeb8cd; }
.date { color: #aeb8cd; font-size: 22rpx; }
.minutes { margin: 34rpx 0 12rpx; }
.big { font-size: 62rpx; font-weight: 900; }
.section { margin-top: 42rpx; }
.link { color: #3658d4; font-size: 24rpx; }
.task { margin-top: 18rpx; padding: 28rpx; }
.task-type { font-size: 30rpx; font-weight: 750; }
.arrow { color: #3658d4; font-size: 38rpx; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; margin-top: 18rpx; }
.entry { min-height: 150rpx; padding: 26rpx; border-radius: 24rpx; display: flex; flex-direction: column; justify-content: space-between; background: #fff; font-size: 27rpx; font-weight: 700; }
.entry-icon { color: #3658d4; font-size: 36rpx; }
.status { display: block; margin-top: 28rpx; color: #7b8498; font-size: 24rpx; }
.error { margin-top: 28rpx; padding: 24rpx; border-radius: 20rpx; background: #fff0f0; color: #a33535; font-size: 24rpx; }
.retry { margin: 16rpx 0 0; background: transparent; color: #3658d4; font-size: 24rpx; }
</style>
