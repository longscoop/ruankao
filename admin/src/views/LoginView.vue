<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { loginAdmin } from '@/api/auth'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入管理员账号和密码')
    return
  }

  loading.value = true
  try {
    const result = await loginAdmin(form.username.trim(), form.password)
    const admin = {
      userId: result.userId,
      username: result.username,
      displayName: result.displayName,
      role: result.role,
    }
    session.setSession(result.token, admin)
    form.password = ''
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-screen">
    <div class="login-visual">
      <div class="login-brand">
        <span class="brand-mark">软</span>
        <div>
          <strong>软考 AI</strong>
          <span>Content Operations</span>
        </div>
      </div>

      <div class="login-copy">
        <span class="eyebrow">ADMIN CONSOLE</span>
        <h1>把资料整理成<br />真正可学习的内容。</h1>
        <p>
          管理 PDF 解析、内容审核、知识点关联和发布流程，
          每一步都有明确状态，不让 AI 自动越权发布。
        </p>
        <div class="login-points">
          <span>PDF 结构化导入</span>
          <span>人工审核闭环</span>
          <span>管理员权限隔离</span>
        </div>
      </div>

      <div class="login-footnote">Soft Exam AI · 系统架构设计师 V1</div>
    </div>

    <div class="login-panel">
      <el-card class="login-card" shadow="never">
        <div class="login-card-head">
          <span class="eyebrow">WELCOME BACK</span>
          <h2>管理员登录</h2>
          <p>使用服务端配置的管理员账号进入内容管理后台。</p>
        </div>

        <el-form label-position="top" @keyup.enter="submit">
          <el-form-item label="管理员账号">
            <el-input
              v-model="form.username"
              size="large"
              autocomplete="username"
              placeholder="请输入管理员账号"
            />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              autocomplete="current-password"
              show-password
              placeholder="请输入密码"
            />
          </el-form-item>

          <el-button
            class="login-submit"
            type="primary"
            size="large"
            :loading="loading"
            @click="submit"
          >
            登录管理后台
          </el-button>
        </el-form>

        <div class="login-hint">
          账号由服务端 <code>ADMIN_USERNAME</code> / <code>ADMIN_PASSWORD</code> 配置。
        </div>
      </el-card>
    </div>
  </section>
</template>
