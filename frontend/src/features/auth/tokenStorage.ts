import type { AuthTokens } from './types'

const STORAGE_KEY = 'personal-agent.auth.v1'

function isAuthTokens(value: unknown): value is AuthTokens {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const tokens = value as Record<string, unknown>
  return typeof tokens.accessToken === 'string'
    && typeof tokens.refreshToken === 'string'
    && typeof tokens.expiresInSeconds === 'number'
}

export const tokenStorage = {
  read(): AuthTokens | null {
    localStorage.removeItem(STORAGE_KEY)
    const value = sessionStorage.getItem(STORAGE_KEY)
    if (value === null) {
      return null
    }
    try {
      const parsed: unknown = JSON.parse(value)
      if (isAuthTokens(parsed)) {
        return parsed
      }
    } catch {
      // 损坏的本地数据不能阻止应用启动，统一清除并按未登录处理。
    }
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  },

  write(tokens: AuthTokens): void {
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(tokens))
  },

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(STORAGE_KEY)
  },
}
