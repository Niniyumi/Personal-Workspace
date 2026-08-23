import { defineStore } from 'pinia'
import { authApi } from './authApi'
import { tokenStorage } from './tokenStorage'
import type { AuthTokens, LoginInput, RegisterInput, User } from './types'

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  status: AuthStatus
  user: User | null
  tokens: AuthTokens | null
  error: string | null
}

function errorCode(error: unknown): string | null {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return null
  }
  const response = (error as { response?: { data?: { code?: unknown } } }).response
  return typeof response?.data?.code === 'string' ? response.data.code : null
}

function userMessage(error: unknown): string {
  switch (errorCode(error)) {
    case 'INVALID_CREDENTIALS':
      return '用户名、邮箱或密码不正确'
    case 'USERNAME_EXISTS':
      return '该用户名已被使用'
    case 'EMAIL_EXISTS':
      return '该邮箱已被使用'
    case 'VALIDATION_ERROR':
      return '请检查输入内容'
    default:
      return '操作失败，请稍后重试'
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    status: 'idle',
    user: null,
    tokens: null,
    error: null,
  }),

  actions: {
    clearSession(): void {
      tokenStorage.clear()
      this.tokens = null
      this.user = null
      this.status = 'anonymous'
    },

    async login(input: LoginInput): Promise<void> {
      this.status = 'loading'
      this.error = null
      try {
        const tokens = await authApi.login(input)
        tokenStorage.write(tokens)
        const user = await authApi.currentUser(tokens.accessToken)
        this.tokens = tokens
        this.user = user
        this.status = 'authenticated'
      } catch (error) {
        this.clearSession()
        this.error = userMessage(error)
        throw error
      }
    },

    async register(input: RegisterInput): Promise<User> {
      this.status = 'loading'
      this.error = null
      try {
        const user = await authApi.register(input)
        this.status = 'anonymous'
        return user
      } catch (error) {
        this.status = 'anonymous'
        this.error = userMessage(error)
        throw error
      }
    },

    async restoreSession(): Promise<void> {
      const saved = tokenStorage.read()
      if (saved === null) {
        this.clearSession()
        return
      }
      this.status = 'loading'
      this.error = null
      this.tokens = saved
      try {
        this.user = await authApi.currentUser(saved.accessToken)
        this.status = 'authenticated'
        return
      } catch {
        // Access Token 失效时只刷新一次，避免启动流程出现循环重试。
      }
      try {
        const refreshed = await authApi.refresh(saved.refreshToken)
        tokenStorage.write(refreshed)
        this.tokens = refreshed
        this.user = await authApi.currentUser(refreshed.accessToken)
        this.status = 'authenticated'
      } catch {
        this.clearSession()
      }
    },

    async logout(): Promise<void> {
      const refreshToken = this.tokens?.refreshToken
      this.status = 'loading'
      try {
        if (refreshToken !== undefined) {
          await authApi.logout(refreshToken)
        }
      } catch {
        // 退出是本地优先操作，服务端会话已失效时无需阻止用户离开当前页面。
      } finally {
        // 服务端令牌已失效或网络中断时，本地也必须退出，避免界面卡在登录态。
        this.clearSession()
      }
    },
  },
})
