<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getLesson } from '@/api/content'
import type { AppRequestError } from '@/lib/errors'
import type { LessonDetailDto } from '@/types/api'

const lesson = ref<LessonDetailDto | null>(null)
const loading = ref(false)
const error = ref('')

onLoad(async (query) => {
  const id = Number(query?.id || 0)
  if (!id) {
    error.value = '讲义参数无效'
    return
  }
  loading.value = true
  try {
    lesson.value = await getLesson(id)
  } catch (e) {
    error.value = (e as AppRequestError).message || '讲义加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <view class="ruankao-page">
    <view v-if="lesson">
      <text class="title">{{ lesson.title }}</text>
      <text v-if="lesson.summary" class="summary">{{ lesson.summary }}</text>
      <text v-if="lesson.sourcePageStart" class="source">
        来源 PDF：第 {{ lesson.sourcePageStart }}<template v-if="lesson.sourcePageEnd && lesson.sourcePageEnd !== lesson.sourcePageStart">–{{ lesson.sourcePageEnd }}</template> 页
      </text>

      <view class="blocks">
        <view v-for="block in lesson.blocks" :key="block.id" class="block">
          <text v-if="block.blockType === 'TEXT'" class="text">{{ block.textContent }}</text>
          <view v-else-if="block.blockType === 'TIP' || block.blockType === 'IMPORTANT'" class="callout ruankao-card">
            <text>{{ block.textContent }}</text>
          </view>
          <view v-else-if="block.blockType === 'QUESTION' || block.blockType === 'CASE'" class="case ruankao-card">
            <text>{{ block.textContent }}</text>
          </view>
          <image v-else-if="block.imageUrl" class="source-image" :src="block.imageUrl" mode="widthFix" />
          <text v-if="block.sourcePage" class="page-tag">PDF 第 {{ block.sourcePage }} 页</text>
        </view>
      </view>

      <view v-if="lesson.knowledgeIds.length" class="knowledge ruankao-card">
        <text class="knowledge-title">关联知识点</text>
        <view class="knowledge-actions">
          <button v-for="id in lesson.knowledgeIds" :key="id" class="knowledge-btn" @tap="uni.navigateTo({url:`/pages/knowledge/detail?id=${id}`})">查看知识点 #{{ id }}</button>
        </view>
      </view>
    </view>
    <text v-if="loading" class="muted">加载讲义…</text>
    <text v-if="error" class="error">{{ error }}</text>
  </view>
</template>

<style scoped>
.title{display:block;font-size:42rpx;font-weight:850}.summary{display:block;margin-top:14rpx;color:#667085;font-size:25rpx;line-height:1.7}.source{display:block;margin-top:12rpx;color:#98a2b3;font-size:21rpx}.blocks{margin-top:30rpx}.block{margin-bottom:26rpx}.text{display:block;white-space:pre-wrap;font-size:28rpx;line-height:1.85;color:#273043}.source-image{width:100%;border-radius:18rpx;background:#eef0f4}.page-tag{display:block;margin-top:8rpx;color:#98a2b3;font-size:20rpx}.callout,.case{padding:24rpx;font-size:26rpx;line-height:1.7}.knowledge{margin-top:36rpx;padding:26rpx}.knowledge-title{font-weight:800;font-size:27rpx}.knowledge-actions{margin-top:16rpx;display:flex;flex-direction:column;gap:12rpx}.knowledge-btn{margin:0;background:#f3f5fb;color:#3658d4;font-size:23rpx}.muted{color:#98a2b3}.error{display:block;margin-top:24rpx;color:#c43d3d}
</style>
