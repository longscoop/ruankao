<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ImportCenter from '@/views/ImportCenter.vue'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const tokenInput = ref(session.token)

function saveToken() {
  session.setToken(tokenInput.value)
  ElMessage.success(session.token ? '后台 Bearer Token 已保存' : 'Token 已清除')
}
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div>
        <div class="brand">软考AI · 内容管理</div>
        <div class="subtitle">PDF 题库 / 知识串讲导入中心</div>
      </div>
      <div class="token-box">
        <el-input v-model="tokenInput" type="password" show-password placeholder="管理员 Bearer Token" />
        <el-button type="primary" @click="saveToken">保存</el-button>
      </div>
    </header>
    <ImportCenter />
  </div>
</template>
