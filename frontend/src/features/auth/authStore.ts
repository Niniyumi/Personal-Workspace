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

let refreshPromise: Promise<string> | null = null

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
    case 'INVALID_REGISTRATION_CODE':
      return '验证码无效或已过期'
    case 'VALIDATION_ERROR':
      return '请检查输入内容'
    default:
      return '操作失败，请稍后重试'
  }
}

// 只记录排错所需的请求信息，不记录用户名、邮箱和密码等表单内容。
function logAuthError(operation: string, error: unknown): void {
  const axiosError = error as {
    config?: { method?: string; url?: string }
    response?: {
      status?: number
      data?: { code?: string; message?: string; traceId?: string }
    }
  }
  console.error(`[auth] ${operation} failed`, {
    method: axiosError.config?.method?.toUpperCase(),
    url: axiosError.config?.url,
    status: axiosError.response?.status,
    code: axiosError.response?.data?.code,
    message: axiosError.response?.data?.message,
    traceId: axiosError.response?.data?.traceId,
  })
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

    async refreshAccessToken(): Promise<string> {
      if (refreshPromise !== null) return refreshPromise
      const refreshToken = this.tokens?.refreshToken
      if (!refreshToken) throw new Error('登录状态已失效')
      refreshPromise = authApi.refresh(refreshToken)
        .then((tokens) => {
          tokenStorage.write(tokens)
          this.tokens = tokens
          return tokens.accessToken
        })
        .catch((error) => {
          this.clearSession()
          throw error
        })
        .finally(() => {
          refreshPromise = null
        })
      return refreshPromise
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
        logAuthError('login', error)
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
        logAuthError('register', error)
        this.status = 'anonymous'
        this.error = userMessage(error)
        throw error
      }
    },

    async requestRegistrationCode(email: string): Promise<void> {
      this.error = null
      try {
        await authApi.requestRegistrationCode(email)
      } catch (error) {
        logAuthError('request registration code', error)
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
