<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ElButton, ElInput } from 'element-plus'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../features/auth/authStore'

const store = useAuthStore()
const router = useRouter()
const route = useRoute()
const login = ref('')
const password = ref('')
const pending = computed(() => store.status === 'loading')
const registered = computed(() => route.query.registered === '1')

async function submit() {
  try {
    await store.login({ login: login.value.trim(), password: password.value })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch {
    // Store 已将后端错误转换为用户可读信息，页面只负责展示。
  }
}
</script>

<template>
  <AuthShell title="欢迎回来" description="登录后继续整理课程、周报和个人成果。">
    <p v-if="registered" class="notice" role="status">账号创建成功，现在可以登录。</p>
    <form class="auth-form" @submit.prevent="submit">
      <div class="field">
        <label for="login">用户名或邮箱</label>
        <ElInput
          id="login"
          v-model.trim="login"
          size="large"
          name="login"
          type="text"
          autocomplete="username"
          :prefix-icon="User"
          placeholder="例如 nini 或 nini@example.com"
          required
          autofocus
        />
      </div>
      <div class="field">
        <label for="password">密码</label>
        <ElInput
          id="password"
          v-model="password"
          size="large"
          name="password"
          type="password"
          autocomplete="current-password"
          :prefix-icon="Lock"
          placeholder="输入你的密码"
          show-password
          required
        />
      </div>
      <p v-if="store.error" class="form-error" role="alert">{{ store.error }}</p>
      <ElButton class="primary-button" size="large" round native-type="submit" :loading="pending" :disabled="pending">
        {{ pending ? '正在登录…' : '登录并进入工作台' }}
      </ElButton>
    </form>
    <p class="auth-switch">还没有账号？<RouterLink to="/register">创建账号</RouterLink></p>
  </AuthShell>
</template>
