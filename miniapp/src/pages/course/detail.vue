<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCourse } from '@/api/content'
import { formatDuration } from '@/lib/video'
import type { AppRequestError } from '@/lib/errors'
import type { CourseDetailDto } from '@/types/api'
const course = ref<CourseDetailDto | null>(null); const loading=ref(false); const error=ref('')
onLoad(async q=>{ const id=Number(q?.id||0); if(!id){error.value='课程参数无效';return} loading.value=true; try{course.value=await getCourse(id)}catch(e){error.value=(e as AppRequestError).message||'课程加载失败'}finally{loading.value=false} })
</script>
<template><view class="ruankao-page"><view v-if="course"><text class="title">{{course.title}}</text><text v-if="course.description" class="desc">{{course.description}}</text><view v-for="(chapter,i) in course.chapters" :key="chapter.id" class="chapter"><text class="chapter-title">{{String(i+1).padStart(2,'0')}} · {{chapter.title}}</text><view v-for="lesson in chapter.lessons" :key="`lesson-${lesson.id}`" class="video ruankao-card" @tap="uni.navigateTo({url:`/pages/lesson/detail?id=${lesson.id}`})"><view><text class="name">{{lesson.title}}</text><text class="meta">知识串讲<span v-if="lesson.sourcePageStart"> · PDF 第 {{lesson.sourcePageStart}} 页</span></text></view><text>›</text></view><view v-for="video in chapter.videos" :key="`video-${video.id}`" class="video ruankao-card" @tap="uni.navigateTo({url:`/pages/video/detail?id=${video.id}`})"><view><text class="name">{{video.title}}</text><text class="meta">{{formatDuration(video.durationSeconds)}} · {{video.freeFlag?'可试看':'Pro'}}</text></view><text>›</text></view></view></view><text v-if="loading" class="muted">加载课程…</text><text v-if="error" class="error">{{error}}</text></view></template>
<style scoped>.title{display:block;font-size:42rpx;font-weight:850}.desc{display:block;margin-top:14rpx;color:#667085;font-size:25rpx;line-height:1.7}.chapter{margin-top:38rpx}.chapter-title{font-size:28rpx;font-weight:800}.video{margin-top:16rpx;padding:26rpx;display:flex;align-items:center;justify-content:space-between}.name{display:block;font-size:27rpx;font-weight:700}.meta,.muted{display:block;margin-top:8rpx;color:#98a2b3;font-size:22rpx}.error{display:block;margin-top:24rpx;color:#c43d3d}</style>
