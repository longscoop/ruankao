<script setup lang="ts">
import { ref } from 'vue'
import { loginWithWechat } from '@/api/auth'
import { capabilityForPath } from '@/config/capabilities'
import type { AppRequestError } from '@/lib/errors'
import { useSessionStore } from '@/stores/session'

const loading = ref(false)
const message = ref('')
const capability = capabilityForPath('/api/v1/auth/wechat/login')

async function login() {
  message.value = ''
  if (!capability.availableInCurrentServer) {
    message.value = '当前分支后端尚未提供微信登录接口；小程序登录页已就绪，接口完成后即可联通。'
    return
  }
  loading.value = true
  try {
    const loginResult = await uni.login({ provider: 'weixin' })
    const result = await loginWithWechat(loginResult.code)
    useSessionStore().setSession(result.token, result.userId)
    uni.redirectTo({ url: result.profileCompleted ? '/pages/index/index' : '/pages/onboarding/profile' })
  } catch (error) {
    message.value = (error as AppRequestError).message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="ruankao-page page">
    <view class="hero">
      <text class="eyebrow">SOFT EXAM AI</text>
      <text class="title">今天学什么，交给计划；学没学会，交给证据。</text>
      <text class="desc">围绕知识点串联短视频、练习、错题和 AI 讲解，专注系统架构设计师。</text>
    </view>
    <view class="feature ruankao-card">
      <view><text class="num">01</text><text>先摸底，薄弱点优先</text></view>
      <view><text class="num">02</text><text>每天只看今天该做的</text></view>
      <view><text class="num">03</text><text>掌握度只由学习证据更新</text></view>
    </view>
    <button class="primary" :loading="loading" @tap="login">微信登录</button>
    <text v-if="message" class="notice">{{ message }}</text>
    <text class="privacy">登录即表示你同意仅使用必要信息完成学习服务。</text>
  </view>
</template>

<style scoped>
.page { display: flex; flex-direction: column; justify-content: space-between; gap: 40rpx; }
.hero { padding-top: 76rpx; display: flex; flex-direction: column; }
.eyebrow { color: #3658d4; font-size: 22rpx; font-weight: 800; letter-spacing: 5rpx; }
.title { margin-top: 28rpx; font-size: 52rpx; line-height: 1.3; font-weight: 800; }
.desc { margin-top: 28rpx; color: #667085; font-size: 28rpx; line-height: 1.8; }
.feature { padding: 34rpx; }
.feature view { display: flex; gap: 22rpx; padding: 16rpx 0; font-size: 28rpx; }
.num { color: #3658d4; font-weight: 800; }
.primary { width: 100%; margin-top: auto; border-radius: 24rpx; background: #3658d4; color: #fff; font-weight: 700; }
.notice { padding: 20rpx 24rpx; border-radius: 18rpx; background: #fff6e5; color: #8a5a00; font-size: 24rpx; line-height: 1.6; }
.privacy { text-align: center; color: #98a2b3; font-size: 22rpx; }
</style>
