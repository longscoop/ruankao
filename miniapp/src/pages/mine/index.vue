<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFavoritesStore } from '@/stores/favorites'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const favorites = useFavoritesStore()
const state = computed(() => session.token ? '已登录' : '未登录')
const syncError = ref('')

onShow(async () => {
  if (!session.restored) session.restore()
  favorites.restore()
  syncError.value = ''
  if (session.token) {
    try {
      await favorites.sync()
    } catch {
      syncError.value = '收藏数量同步失败，当前显示最近一次缓存。'
    }
  }
})

function logout() {
  session.clear()
  favorites.clearCache()
  uni.showToast({ title: '已退出登录', icon: 'none' })
}
</script>

<template>
  <view class="ruankao-page">
    <view class="profile ruankao-card">
      <view class="avatar">我</view>
      <view>
        <text class="title">我的学习</text>
        <text class="muted">{{ state }}</text>
      </view>
    </view>

    <view class="menu ruankao-card">
      <view class="row" @tap="uni.navigateTo({ url: '/pages/report/weekly' })">
        <text>本周学习报告</text><text>›</text>
      </view>
      <view class="row" @tap="uni.navigateTo({ url: '/pages/practice/favorites' })">
        <text>我的收藏</text><text class="right">{{ favorites.items.length }} 道 ›</text>
      </view>
      <view class="row" @tap="uni.navigateTo({ url: '/pages/onboarding/profile' })">
        <text>学习设置</text><text>›</text>
      </view>
    </view>

    <text v-if="syncError" class="sync-error">{{ syncError }}</text>
    <text class="privacy">学习数据和收藏均以当前登录账号的服务端记录为准。</text>
    <button v-if="session.token" class="logout" @tap="logout">退出登录</button>
  </view>
</template>

<style scoped>
.profile{padding:32rpx;display:flex;align-items:center;gap:22rpx}.avatar{width:80rpx;height:80rpx;border-radius:26rpx;background:#172033;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:800}.title{display:block;font-size:31rpx;font-weight:850}.muted{display:block;margin-top:7rpx;color:#98a2b3;font-size:22rpx}.menu{margin-top:26rpx;padding:0 28rpx}.row{min-height:100rpx;display:flex;align-items:center;justify-content:space-between;border-bottom:1rpx solid #eef0f4;font-size:27rpx}.row:last-child{border:0}.right{color:#667085}.privacy,.sync-error{display:block;margin-top:24rpx;color:#98a2b3;font-size:22rpx;line-height:1.6}.sync-error{color:#9b5c00}.logout{margin-top:34rpx;background:transparent;color:#c43d3d;font-size:25rpx}
</style>
