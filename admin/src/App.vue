<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentAdmin, logoutAdmin } from '@/api/auth'
import { useSessionStore } from '@/stores/session'
import ImportCenter from '@/views/ImportCenter.vue'
import LoginView from '@/views/LoginView.vue'

const session = useSessionStore()
const checkingSession = ref(Boolean(session.token))
const displayName = computed(
  () => session.admin?.displayName || session.admin?.username || '管理员',
)

async function verifySession() {
  if (!session.token) {
    checkingSession.value = false
    return
  }

  checkingSession.value = true
  try {
    session.setAdmin(await getCurrentAdmin())
  } catch {
    session.clear()
  } finally {
    checkingSession.value = false
  }
}

async function signOut() {
  try {
    await logoutAdmin()
  } catch {
    // Local logout still takes effect if the server is temporarily unreachable.
  } finally {
    session.clear()
    ElMessage.success('已退出登录')
  }
}

function onUnauthorized() {
  checkingSession.value = false
  ElMessage.warning('登录状态已失效，请重新登录')
}

onMounted(() => {
  window.addEventListener('ruankao:unauthorized', onUnauthorized)
  verifySession()
})

onBeforeUnmount(() => {
  window.removeEventListener('ruankao:unauthorized', onUnauthorized)
})
</script>

<template>
  <div v-if="checkingSession" class="boot-screen">
    <div class="boot-logo">软</div>
    <div>
      <strong>正在验证登录状态</strong>
      <span>请稍候…</span>
    </div>
  </div>

  <LoginView v-else-if="!session.isAuthenticated" />

  <div v-else class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <span class="brand-mark small">软</span>
        <div>
          <strong>软考 AI</strong>
          <span>内容管理后台</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-group-title">内容运营</div>
        <button class="nav-item active" type="button">
          <span class="nav-icon">导</span>
          <span>
            <strong>内容导入</strong>
            <small>PDF 审核与发布</small>
          </span>
        </button>

        <div class="nav-group-title secondary">系统</div>
        <div class="nav-item muted">
          <span class="nav-icon">权</span>
          <span>
            <strong>权限已启用</strong>
            <small>ROLE_ADMIN</small>
          </span>
        </div>
      </nav>

      <div class="sidebar-bottom">
        <div class="environment-badge">
          <span class="status-dot"></span>
          管理员安全会话
        </div>
        <span>Soft Exam AI V1</span>
      </div>
    </aside>

    <div class="admin-main">
      <header class="app-topbar">
        <div>
          <span class="breadcrumb">内容运营 / PDF 导入</span>
          <h1>内容导入中心</h1>
        </div>

        <div class="account-area">
          <div class="account-avatar">{{ displayName.slice(0, 1).toUpperCase() }}</div>
          <div class="account-copy">
            <strong>{{ displayName }}</strong>
            <span>{{ session.admin?.username }}</span>
          </div>
          <el-button text @click="signOut">退出</el-button>
        </div>
      </header>

      <ImportCenter />
    </div>
  </div>
</template>
