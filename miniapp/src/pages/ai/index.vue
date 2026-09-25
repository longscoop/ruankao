<script setup lang="ts">
import { ref } from 'vue'
import { askAi } from '@/api/ai'
import { capabilityForPath } from '@/config/capabilities'
import type { AppRequestError } from '@/lib/errors'
const capability = capabilityForPath('/api/v1/ai/chat')
const input = ref('')
const answer = ref('')
const error = ref('')
const loading = ref(false)
async function send() {
  const message = input.value.trim()
  if (!message || loading.value) return
  error.value = ''; answer.value = ''
  if (!capability.availableInCurrentServer) { error.value = '当前 AI 服务不可用，你仍可继续正常学习'; return }
  loading.value = true
  try { answer.value = (await askAi(message)).content }
  catch (e) { error.value = (e as AppRequestError).message || 'AI 暂时不可用，你仍可继续正常学习' }
  finally { loading.value = false }
}
function openAgents() { uni.navigateTo({ url: '/pages/ai/agents' }) }
</script>
<template>
  <view class="ruankao-page">
    <text class="eyebrow">AI TUTOR</text><text class="title">不会的地方，换一种讲法</text>
    <text class="desc">AI 用于解释、举例和学习建议，不计算掌握度，也不会改标准答案。</text>
    <view class="material-entry ruankao-card"><text class="entry-title">依据学习资料提问</text><text class="desc">选择资料智能体，连续追问并查看文件来源与原文。</text><button @tap="openAgents">进入资料智能体</button></view>
    <textarea v-model="input" class="input ruankao-card" placeholder="输入你想理解的问题…" maxlength="1000" :disabled="loading" />
    <button class="primary" :loading="loading" :disabled="loading || !input.trim()" @tap="send">问 AI</button>
    <view v-if="answer" class="answer ruankao-card">{{ answer }}</view><view v-if="error" class="error">{{ error }}</view>
  </view>
</template>
<style scoped>
.eyebrow{color:#7a42d8;font-size:22rpx;font-weight:800;letter-spacing:4rpx}.title{display:block;margin-top:12rpx;font-size:42rpx;line-height:1.4;font-weight:850}.desc{display:block;margin-top:18rpx;color:#667085;font-size:25rpx;line-height:1.7}.input{width:100%;box-sizing:border-box;min-height:240rpx;margin-top:28rpx;padding:28rpx;font-size:27rpx}.primary{margin-top:20rpx;border-radius:22rpx;background:#172033;color:#fff;font-weight:700}.answer{margin-top:28rpx;padding:30rpx;color:#344054;font-size:26rpx;line-height:1.75;white-space:pre-wrap}.error{margin-top:24rpx;color:#9b5c00;font-size:24rpx;line-height:1.6}.material-entry{margin-top:26rpx;padding:28rpx}.entry-title{font-size:29rpx;font-weight:750}.material-entry button{margin-top:20rpx;background:#eee9fb;color:#583b9d;font-size:26rpx;border-radius:18rpx}
</style>
