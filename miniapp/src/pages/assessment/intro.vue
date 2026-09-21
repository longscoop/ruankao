<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { startAssessment } from '@/api/assessment'
import type { AppRequestError } from '@/lib/errors'

const examId = ref(0)
const loading = ref(false)
const error = ref('')

onLoad((query) => {
  examId.value = Number(query?.examId || 0)
})

async function start() {
  loading.value = true
  error.value = ''
  try {
    const result = await startAssessment(examId.value, 20)
    uni.redirectTo({ url: `/pages/assessment/session?sessionId=${result.sessionId}` })
  } catch (e) {
    error.value = (e as AppRequestError).message || '摸底测试暂时无法开始'
  } finally {
    loading.value = false
  }
}

function skip() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="ruankao-page page">
    <view>
      <text class="step">2 / 2</text>
      <text class="title">用 20 道题找到真正的起点</text>
      <text class="desc">摸底可跳过。没有答题证据的知识点会显示“待评估”，不会用默认百分比冒充掌握度。</text>
    </view>
    <view class="ruankao-card card">
      <text class="big">约 15 分钟</text>
      <text>答题时可以标记“蒙的 / 不确定 / 确定”，帮助学习引擎判断证据强度。</text>
    </view>
    <text v-if="error" class="error">{{ error }}</text>
    <button class="primary" :loading="loading" @tap="start">开始摸底</button>
    <button class="ghost" @tap="skip">先跳过，直接学习</button>
  </view>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 28rpx; }
.step { color: #3658d4; font-size: 24rpx; font-weight: 700; }
.title { display: block; margin-top: 20rpx; font-size: 44rpx; line-height: 1.35; font-weight: 800; }
.desc { display: block; margin-top: 22rpx; color: #667085; line-height: 1.75; font-size: 27rpx; }
.card { padding: 34rpx; color: #475467; line-height: 1.7; font-size: 26rpx; }
.big { display: block; margin-bottom: 14rpx; color: #172033; font-size: 36rpx; font-weight: 800; }
.error { color: #c43d3d; font-size: 24rpx; }
.primary, .ghost { border-radius: 24rpx; font-weight: 700; }
.primary { margin-top: auto; background: #3658d4; color: #fff; }
.ghost { background: transparent; color: #667085; }
</style>
