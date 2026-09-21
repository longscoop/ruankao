<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listCourses } from '@/api/content'
import { capabilityForPath } from '@/config/capabilities'
import type { AppRequestError } from '@/lib/errors'
import type { CourseSummaryDto } from '@/types/api'

const courses = ref<CourseSummaryDto[]>([])
const loading = ref(false)
const error = ref('')
const capability = capabilityForPath('/api/v1/courses')

async function load() {
  loading.value = true
  error.value = ''
  try { courses.value = await listCourses() }
  catch (e) { error.value = (e as AppRequestError).message || '课程暂时无法加载' }
  finally { loading.value = false }
}
onShow(load)
</script>

<template>
  <view class="ruankao-page">
    <text class="eyebrow">COURSES</text><text class="title">按知识体系，一节一节学</text>
    <view v-if="!capability.availableInCurrentServer" class="notice">当前分支已完成课程/视频领域服务，但 REST 接口尚未暴露；页面会在接口上线后直接使用真实数据。</view>
    <view v-if="courses.length" class="list">
      <view v-for="course in courses" :key="course.id" class="ruankao-card card" @tap="uni.navigateTo({ url: `/pages/course/detail?id=${course.id}` })">
        <text class="name">{{ course.title }}</text><text v-if="course.description" class="desc">{{ course.description }}</text><text class="more">查看章节 →</text>
      </view>
    </view>
    <view v-else-if="!loading && !error" class="ruankao-card empty">暂无已发布课程</view>
    <text v-if="loading" class="status">加载课程…</text><text v-if="error" class="error">{{ error }}</text>
  </view>
</template>
<style scoped>
.eyebrow{color:#3658d4;font-size:22rpx;font-weight:800;letter-spacing:4rpx}.title{display:block;margin:12rpx 0 30rpx;font-size:40rpx;font-weight:850}.notice{padding:24rpx;border-radius:20rpx;background:#fff6e5;color:#8a5a00;font-size:24rpx;line-height:1.6}.list{display:flex;flex-direction:column;gap:18rpx;margin-top:28rpx}.card{padding:30rpx}.name{display:block;font-size:31rpx;font-weight:800}.desc{display:block;margin-top:12rpx;color:#667085;font-size:24rpx;line-height:1.65}.more{display:block;margin-top:22rpx;color:#3658d4;font-size:24rpx;font-weight:700}.empty{margin-top:28rpx;padding:42rpx;text-align:center;color:#98a2b3}.status,.error{display:block;margin-top:26rpx;font-size:24rpx}.status{color:#7b8498}.error{color:#c43d3d}
</style>
