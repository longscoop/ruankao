<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const total = ref(0)
const correct = ref(0)
const percent = computed(() => total.value ? Math.round(correct.value / total.value * 100) : 0)

onLoad((query) => {
  total.value = Number(query?.total || 0)
  correct.value = Number(query?.correct || 0)
})
</script>

<template>
  <view class="ruankao-page page">
    <text class="eyebrow">摸底完成</text>
    <text class="score">{{ correct }} / {{ total }}</text>
    <text class="percent">正确率 {{ percent }}%</text>
    <view class="ruankao-card note">掌握度会根据每一道题关联的知识点、难度、把握程度和连续答题证据由服务端 Learning Engine 更新。</view>
    <button class="primary" @tap="uni.reLaunch({ url: '/pages/index/index' })">进入今日学习</button>
  </view>
</template>

<style scoped>
.page { display: flex; flex-direction: column; align-items: center; text-align: center; padding-top: 120rpx; }
.eyebrow { color: #16a36a; font-size: 26rpx; font-weight: 800; }
.score { margin-top: 28rpx; font-size: 88rpx; font-weight: 900; }
.percent { margin-top: 10rpx; color: #667085; font-size: 28rpx; }
.note { margin-top: 48rpx; padding: 30rpx; color: #475467; text-align: left; font-size: 25rpx; line-height: 1.7; }
.primary { width: 100%; margin-top: auto; border-radius: 24rpx; background: #3658d4; color: #fff; font-weight: 700; }
</style>
