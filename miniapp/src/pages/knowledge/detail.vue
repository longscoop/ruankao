<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getKnowledge } from '@/api/content'
import type { AppRequestError } from '@/lib/errors'
import type { KnowledgeDetailDto } from '@/types/api'
const knowledge=ref<KnowledgeDetailDto|null>(null); const error=ref(''); const loading=ref(false)
onLoad(async q=>{const id=Number(q?.id||0);if(!id){error.value='知识点参数无效';return}loading.value=true;try{knowledge.value=await getKnowledge(id)}catch(e){error.value=(e as AppRequestError).message||'知识点加载失败'}finally{loading.value=false}})
</script>
<template><view class="ruankao-page"><view v-if="knowledge"><text class="eyebrow">KNOWLEDGE</text><text class="title">{{knowledge.name}}</text><view class="mastery ruankao-card"><text class="label">当前掌握度</text><text class="score">{{knowledge.evidenceCount>0 && knowledge.masteryScore!=null ? `${Math.round(knowledge.masteryScore)}%` : '待评估'}}</text><text class="hint">没有有效学习证据时不显示默认百分比。</text></view><text v-if="knowledge.description" class="desc">{{knowledge.description}}</text><view v-if="knowledge.videos?.length" class="videos"><text class="section">相关视频</text><view v-for="video in knowledge.videos" :key="video.id" class="ruankao-card item" @tap="uni.navigateTo({url:`/pages/video/detail?id=${video.id}`})">{{video.title}}<text>›</text></view></view></view><text v-if="loading" class="muted">加载知识点…</text><text v-if="error" class="error">{{error}}</text></view></template>
<style scoped>.eyebrow{color:#3658d4;font-size:22rpx;font-weight:800;letter-spacing:4rpx}.title{display:block;margin-top:12rpx;font-size:42rpx;font-weight:850}.mastery{margin-top:30rpx;padding:30rpx}.label,.hint,.muted{display:block;color:#7b8498;font-size:23rpx}.score{display:block;margin-top:12rpx;font-size:48rpx;font-weight:900}.hint{margin-top:8rpx}.desc{display:block;margin-top:30rpx;color:#475467;font-size:26rpx;line-height:1.75}.videos{margin-top:38rpx}.section{font-size:29rpx;font-weight:800}.item{display:flex;justify-content:space-between;margin-top:15rpx;padding:25rpx;font-size:26rpx}.error{display:block;margin-top:24rpx;color:#c43d3d}</style>
