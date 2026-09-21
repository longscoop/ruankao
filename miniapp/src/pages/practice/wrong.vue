<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listWrongQuestions, startQuestionSession } from '@/api/practice'
import type { AppRequestError } from '@/lib/errors'
import type { WrongQuestionDto } from '@/types/api'

const items=ref<WrongQuestionDto[]>([])
const error=ref('')
const loading=ref(false)
const starting=ref(false)

async function load(){
  loading.value=true
  error.value=''
  try{items.value=await listWrongQuestions()}
  catch(e){error.value=(e as AppRequestError).message||'错题本加载失败'}
  finally{loading.value=false}
}

async function startReview(){
  starting.value=true
  error.value=''
  try{
    const result=await startQuestionSession({source:'WRONG_REVIEW'})
    uni.navigateTo({url:`/pages/practice/session?sessionId=${result.sessionId}&source=WRONG_REVIEW`})
  }catch(e){
    error.value=(e as AppRequestError).message||'错题复习暂时无法开始'
  }finally{
    starting.value=false
  }
}

onShow(load)
</script>

<template>
  <view class="ruankao-page">
    <view class="head">
      <text class="title">错题本</text>
      <button v-if="items.some(item=>item.status==='ACTIVE')" class="review" :loading="starting" @tap="startReview">开始复习</button>
    </view>
    <view v-if="items.length" class="list">
      <view v-for="item in items" :key="item.id" class="item ruankao-card">
        <view class="top"><text>{{item.status==='ACTIVE'?'待复习':'已掌握'}}</text><text>错 {{item.wrongCount}} 次</text></view>
        <text class="content">{{item.question?.content || `题目 #${item.questionId}`}}</text>
      </view>
    </view>
    <view v-else-if="!loading&&!error" class="empty ruankao-card">暂无错题记录</view>
    <text v-if="error" class="error">{{error}}</text>
  </view>
</template>

<style scoped>
.head{display:flex;align-items:center;justify-content:space-between}.title{display:block;font-size:42rpx;font-weight:850}.review{margin:0;border-radius:999rpx;background:#3658d4;color:#fff;font-size:23rpx;font-weight:700}.list{margin-top:24rpx;display:flex;flex-direction:column;gap:16rpx}.item{padding:26rpx}.top{display:flex;justify-content:space-between;color:#667085;font-size:22rpx}.content{display:block;margin-top:14rpx;font-size:27rpx;line-height:1.65}.empty{margin-top:28rpx;padding:42rpx;text-align:center;color:#98a2b3}.error{display:block;margin-top:24rpx;color:#c43d3d}
</style>
