<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getVideo, updateVideoProgress } from '@/api/content'
import { formatDuration, isVideoComplete, shouldReportProgress, videoProgressPercent } from '@/lib/video'
import type { AppRequestError } from '@/lib/errors'
import type { VideoDetailDto } from '@/types/api'

const video=ref<VideoDetailDto|null>(null); const loading=ref(false); const error=ref(''); const current=ref(0); const lastReported=ref(0)
const percent=computed(()=>video.value?videoProgressPercent(current.value,video.value.durationSeconds):0)
const complete=computed(()=>video.value?isVideoComplete(current.value,video.value.durationSeconds):false)

onLoad(async q=>{const id=Number(q?.id||0);if(!id){error.value='视频参数无效';return}loading.value=true;try{video.value=await getVideo(id);current.value=video.value.progressSeconds||0;lastReported.value=current.value}catch(e){error.value=(e as AppRequestError).message||'视频加载失败'}finally{loading.value=false}})
async function report(force=false){if(!video.value)return;if(!force&&!shouldReportProgress(lastReported.value,current.value,video.value.durationSeconds))return;try{await updateVideoProgress(video.value.id,current.value);lastReported.value=current.value}catch{}}
function timeupdate(e: Event){const detail=(e as unknown as {detail:{currentTime:number}}).detail;current.value=detail.currentTime;void report(false)}
function ended(){if(video.value){current.value=video.value.durationSeconds;void report(true)}}
onUnload(()=>{void report(true)})
</script>
<template><view class="ruankao-page"><view v-if="video"><video v-if="video.playUrl" class="player" :src="video.playUrl" :initial-time="current" controls @timeupdate="timeupdate" @ended="ended"/><view v-else class="player missing">播放地址尚未由后端提供</view><text class="title">{{video.title}}</text><view class="progress"><view class="row"><text>{{formatDuration(current)}} / {{formatDuration(video.durationSeconds)}}</text><text>{{percent}}%</text></view><view class="bar"><view class="inner" :style="{width:percent+'%'}"/></view><text class="hint">观看达到 85% 才记为完成；完成只提供弱掌握度证据。</text><text v-if="complete" class="done">已达到完成阈值</text></view><text v-if="video.description" class="desc">{{video.description}}</text><view v-if="video.transcript?.length" class="transcript"><text class="section-title">字幕</text><view v-for="segment in video.transcript" :key="segment.startSeconds" class="line"><text class="time">{{formatDuration(segment.startSeconds)}}</text><text>{{segment.text}}</text></view></view></view><text v-if="loading" class="muted">加载视频…</text><text v-if="error" class="error">{{error}}</text></view></template>
<style scoped>.player{width:100%;height:390rpx;border-radius:26rpx;overflow:hidden;background:#111}.missing{display:flex;align-items:center;justify-content:center;color:#aeb8cd;font-size:24rpx}.title{display:block;margin-top:28rpx;font-size:36rpx;font-weight:850}.progress{margin-top:24rpx}.row{display:flex;justify-content:space-between;color:#667085;font-size:22rpx}.bar{height:10rpx;margin-top:10rpx;border-radius:999rpx;background:#e8ebf2;overflow:hidden}.inner{height:100%;background:#3658d4}.hint,.desc,.muted{display:block;margin-top:14rpx;color:#7b8498;font-size:23rpx;line-height:1.65}.done{display:block;margin-top:10rpx;color:#16a36a;font-size:23rpx;font-weight:700}.desc{margin-top:30rpx;color:#475467}.transcript{margin-top:38rpx}.section-title{font-size:29rpx;font-weight:800}.line{display:flex;gap:20rpx;padding:18rpx 0;border-bottom:1rpx solid #eef0f4;color:#475467;font-size:24rpx;line-height:1.6}.time{min-width:82rpx;color:#3658d4}.error{display:block;margin-top:24rpx;color:#c43d3d}</style>
