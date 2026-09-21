<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useFavoritesStore } from '@/stores/favorites'
const favorites=useFavoritesStore();onShow(()=>favorites.restore())
</script>
<template><view class="ruankao-page"><text class="title">我的收藏</text><text class="tip">V1 当前为本机收藏，仅保存你真实加载并主动收藏过的题目；服务端收藏接口上线后再迁移为云同步。</text><view v-if="favorites.items.length" class="list"><view v-for="item in favorites.items" :key="item.question.id" class="ruankao-card card"><text class="meta">{{item.question.type}} · {{item.question.difficulty}}</text><text class="content">{{item.question.content}}</text><button class="remove" @tap="favorites.toggle(item.question)">取消收藏</button></view></view><view v-else class="ruankao-card empty">还没有收藏题目</view></view></template>
<style scoped>.title{display:block;font-size:42rpx;font-weight:850}.tip{display:block;margin-top:16rpx;color:#7b8498;font-size:23rpx;line-height:1.65}.list{display:flex;flex-direction:column;gap:16rpx;margin-top:28rpx}.card{padding:26rpx}.meta{color:#667085;font-size:21rpx}.content{display:block;margin-top:13rpx;font-size:27rpx;line-height:1.65}.remove{margin:18rpx 0 0;background:transparent;color:#c43d3d;font-size:22rpx}.empty{margin-top:28rpx;padding:42rpx;text-align:center;color:#98a2b3}</style>
