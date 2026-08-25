<script setup lang="ts">
import axios from 'axios'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { authApi } from '../features/auth/authApi'
import { logApiError } from '../shared/logApiError'

const router = useRouter()
const step = ref<'email' | 'code'>('email')
const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const pending = ref(false)
const error = ref<string | null>(null)
const notice = ref<string | null>(null)

async function submit() {
  error.value = null
  if (step.value === 'code' && newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  pending.value = true
  try {
    if (step.value === 'email') {
      await authApi.requestPasswordReset(email.value.trim())
      step.value = 'code'
      notice.value = '如果该邮箱已注册，验证码已经发送。'
      return
    }
    await authApi.confirmPasswordReset({
      email: email.value.trim(), code: code.value.trim(), newPassword: newPassword.value,
    })
    await router.push({ name: 'login', query: { reset: '1' } })
  } catch (cause) {
    logApiError('password-reset', cause)
    const responseCode = axios.isAxiosError(cause)
      ? (cause.response?.data as { code?: string } | undefined)?.code
      : undefined
    error.value = responseCode === 'INVALID_RESET_CODE' ? '验证码错误或已过期' : '操作失败，请稍后重试'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <AuthShell title="找回密码" description="通过注册邮箱验证身份并设置新密码。">
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <form class="auth-form" @submit.prevent="submit">
      <div class="field">
        <label for="reset-email">注册邮箱</label>
        <input id="reset-email" v-model.trim="email" type="email" name="email" autocomplete="email" required :readonly="step === 'code'" />
      </div>
      <template v-if="step === 'code'">
        <div class="field">
          <label for="reset-code">6 位验证码</label>
          <input id="reset-code" v-model.trim="code" name="code" inputmode="numeric" pattern="\d{6}" maxlength="6" required />
        </div>
        <div class="field">
          <label for="new-password">新密码</label>
          <input id="new-password" v-model="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" required />
        </div>
        <div class="field">
          <label for="confirm-password">确认新密码</label>
          <input id="confirm-password" v-model="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" required />
        </div>
      </template>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button" type="submit" :disabled="pending">
        {{ pending ? '正在处理…' : step === 'email' ? '发送验证码' : '确认修改密码' }}
      </button>
    </form>
    <p class="auth-switch"><RouterLink to="/login">返回登录</RouterLink></p>
  </AuthShell>
</template>
