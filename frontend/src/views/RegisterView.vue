<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../features/auth/authStore'

const store = useAuthStore()
const router = useRouter()
const username = ref('')
const email = ref('')
const displayName = ref('')
const password = ref('')
const pending = computed(() => store.status === 'loading')

async function submit() {
  try {
    await store.register({
      username: username.value.trim(),
      email: email.value.trim(),
      displayName: displayName.value.trim(),
      password: password.value,
    })
    await router.push({ name: 'login', query: { registered: '1' } })
  } catch {
    // Store 已处理错误映射，保留表单内容方便用户修改。
  }
}
</script>

<template>
  <AuthShell title="创建账号" description="用一个账号保存属于你的学习与工作成果。">
    <form class="auth-form" @submit.prevent="submit">
      <div class="field-row">
        <div class="field">
          <label for="username">用户名</label>
          <input id="username" v-model.trim="username" name="username" autocomplete="username" minlength="3" maxlength="50" pattern="[^@]+" required />
        </div>
        <div class="field">
          <label for="displayName">显示名称</label>
          <input id="displayName" v-model.trim="displayName" name="displayName" autocomplete="name" maxlength="80" required />
        </div>
      </div>
      <div class="field">
        <label for="email">邮箱</label>
        <input id="email" v-model.trim="email" name="email" type="email" autocomplete="email" maxlength="255" required />
      </div>
      <div class="field">
        <label for="password">密码</label>
        <input id="password" v-model="password" name="password" type="password" autocomplete="new-password" minlength="8" required />
        <small>至少 8 个字符；支持中文，但总长度不能超过 BCrypt 的 72 字节限制。</small>
      </div>
      <p v-if="store.error" class="form-error" role="alert">{{ store.error }}</p>
      <button class="primary-button" type="submit" :disabled="pending">
        {{ pending ? '正在创建…' : '创建账号' }}
      </button>
    </form>
    <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
  </AuthShell>
</template>
