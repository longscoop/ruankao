<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFavoritesStore } from '@/stores/favorites'

const favorites = useFavoritesStore()
const error = ref('')

onShow(async () => {
  favorites.restore()
  try {
    await favorites.sync()
  } catch {
    error.value = '云端收藏同步失败，当前显示最近一次缓存。'
  }
})

async function remove(questionId: number) {
  const item = favorites.items.find((entry) => entry.question.id === questionId)
  if (!item) return
  try {
    await favorites.toggle(item.question)
  } catch {
    error.value = '取消收藏失败，请稍后重试。'
  }
}
</script>

<template>
  <view class="ruankao-page">
    <text class="title">我的收藏</text>
    <text class="tip">收藏已同步到账号，换设备登录后也可以继续查看。</text>
    <text v-if="error" class="error">{{ error }}</text>
    <view v-if="favorites.items.length" class="list">
      <view v-for="item in favorites.items" :key="item.question.id" class="ruankao-card card">
        <text class="meta">{{ item.question.type }} · {{ item.question.difficulty }}</text>
        <text class="content">{{ item.question.content }}</text>
        <button class="remove" @tap="remove(item.question.id)">取消收藏</button>
      </view>
    </view>
    <view v-else-if="!favorites.syncing" class="ruankao-card empty">还没有收藏题目</view>
  </view>
</template>

<style scoped>
.title{display:block;font-size:42rpx;font-weight:850}.tip{display:block;margin-top:16rpx;color:#7b8498;font-size:23rpx;line-height:1.65}.error{display:block;margin-top:14rpx;color:#c43d3d;font-size:22rpx}.list{display:flex;flex-direction:column;gap:16rpx;margin-top:28rpx}.card{padding:26rpx}.meta{color:#667085;font-size:21rpx}.content{display:block;margin-top:13rpx;font-size:27rpx;line-height:1.65}.remove{margin:18rpx 0 0;background:transparent;color:#c43d3d;font-size:22rpx}.empty{margin-top:28rpx;padding:42rpx;text-align:center;color:#98a2b3}
</style>
