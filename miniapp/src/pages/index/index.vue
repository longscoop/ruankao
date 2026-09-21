<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useSessionStore } from '@/stores/session'

onShow(() => {
  const session = useSessionStore()
  if (!session.restored) session.restore()
})
</script>

<template>
  <view class="ruankao-page landing">
    <view class="brand">软考AI</view>
    <text class="title">把每一天学到点上</text>
    <text class="subtitle">系统架构设计师 · 个性化学习</text>
    <button class="primary" @tap="uni.navigateTo({ url: '/pages/onboarding/welcome' })">开始设置</button>
  </view>
</template>

<style scoped>
.landing { display: flex; flex-direction: column; justify-content: center; }
.brand { width: max-content; padding: 10rpx 18rpx; border-radius: 999rpx; background: #eaf0ff; color: #3658d4; font-size: 24rpx; font-weight: 700; }
.title { margin-top: 28rpx; font-size: 54rpx; line-height: 1.25; font-weight: 800; }
.subtitle { margin-top: 16rpx; color: #77829a; font-size: 28rpx; }
.primary { margin-top: 52rpx; border-radius: 24rpx; background: #3658d4; color: #fff; font-weight: 700; }
</style>
