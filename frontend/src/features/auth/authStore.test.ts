import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authApi'
import { useAuthStore } from './authStore'
import { tokenStorage } from './tokenStorage'
import type { AuthTokens, User } from './types'

vi.mock('./authApi', () => ({
  authApi: {
    register: vi.fn(),
    requestRegistrationCode: vi.fn(),
    login: vi.fn(),
    currentUser: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}))

const tokens: AuthTokens = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  expiresInSeconds: 900,
}

const user: User = {
  id: 42,
  username: 'nini',
  email: 'nini@example.com',
  displayName: 'Nini',
}

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('logs in, persists tokens, and loads the current user', async () => {
    vi.mocked(authApi.login).mockResolvedValue(tokens)
    vi.mocked(authApi.currentUser).mockResolvedValue(user)
    const store = useAuthStore()

    await store.login({ login: 'nini', password: 'UnitTest7!' })

    expect(store.status).toBe('authenticated')
    expect(store.user).toEqual(user)
    expect(store.tokens).toEqual(tokens)
    expect(tokenStorage.read()).toEqual(tokens)
  })

  it('exposes a stable message and anonymous state when login fails', async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      response: { data: { code: 'INVALID_CREDENTIALS', message: 'Invalid credentials', traceId: 'trace-1' } },
    })
    const store = useAuthStore()

    await expect(store.login({ login: 'nini', password: 'wrong-pass' })).rejects.toBeTruthy()

    expect(store.status).toBe('anonymous')
    expect(store.error).toBe('用户名、邮箱或密码不正确')
    expect(tokenStorage.read()).toBeNull()
  })

  it('restores a saved session without refreshing when the access token works', async () => {
    tokenStorage.write(tokens)
    vi.mocked(authApi.currentUser).mockResolvedValue(user)
    const store = useAuthStore()

    await store.restoreSession()

    expect(store.status).toBe('authenticated')
    expect(store.user).toEqual(user)
    expect(authApi.refresh).not.toHaveBeenCalled()
  })

  it('refreshes once and retries current user when the saved access token fails', async () => {
    const refreshed = { ...tokens, accessToken: 'new-access-token', refreshToken: 'new-refresh-token' }
    tokenStorage.write(tokens)
    vi.mocked(authApi.currentUser)
      .mockRejectedValueOnce(new Error('expired'))
      .mockResolvedValueOnce(user)
    vi.mocked(authApi.refresh).mockResolvedValue(refreshed)
    const store = useAuthStore()

    await store.restoreSession()

    expect(authApi.refresh).toHaveBeenCalledOnce()
    expect(authApi.currentUser).toHaveBeenLastCalledWith('new-access-token')
    expect(tokenStorage.read()).toEqual(refreshed)
    expect(store.status).toBe('authenticated')
  })

  it('clears an unusable saved session after one failed refresh', async () => {
    tokenStorage.write(tokens)
    vi.mocked(authApi.currentUser).mockRejectedValue(new Error('expired'))
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('refresh rejected'))
    const store = useAuthStore()

    await store.restoreSession()

    expect(authApi.refresh).toHaveBeenCalledOnce()
    expect(tokenStorage.read()).toBeNull()
    expect(store.status).toBe('anonymous')
    expect(store.sessionExpired).toBe(true)
  })

  it('marks the session expired when an api refresh is rejected', async () => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('refresh rejected'))
    const store = useAuthStore()
    store.tokens = tokens

    await expect(store.refreshAccessToken()).rejects.toThrow('refresh rejected')

    expect(store.sessionExpired).toBe(true)
    expect(store.status).toBe('anonymous')
    expect(store.tokens).toBeNull()
  })

  it('registers a user without creating a local session', async () => {
    vi.mocked(authApi.register).mockResolvedValue(user)
    const store = useAuthStore()

    const result = await store.register({
      username: 'nini',
      email: 'nini@example.com',
      password: 'UnitTest7!',
      displayName: 'Nini',
      code: '123456',
    })

    expect(result).toEqual(user)
    expect(store.status).toBe('anonymous')
    expect(tokenStorage.read()).toBeNull()
  })

  it('requests a registration code without changing login state', async () => {
    vi.mocked(authApi.requestRegistrationCode).mockResolvedValue(undefined)
    const store = useAuthStore()

    await store.requestRegistrationCode('nini@example.com')

    expect(authApi.requestRegistrationCode).toHaveBeenCalledWith('nini@example.com')
    expect(store.status).toBe('idle')
  })

  it('shows an actionable message when registration email cannot be sent', async () => {
    const failure = {
      response: { data: { code: 'VERIFICATION_MAIL_SEND_FAILED' } },
    }
    vi.mocked(authApi.requestRegistrationCode).mockRejectedValue(failure)
    const store = useAuthStore()

    await expect(store.requestRegistrationCode('nini@example.com')).rejects.toBe(failure)

    expect(store.error).toBe('验证码发送失败，请稍后重试；仍失败请联系管理员')
  })

  it('logs the original error when registration fails', async () => {
    const failure = {
      response: {
        status: 404,
        data: { code: 'NOT_FOUND', message: 'Not Found', traceId: 'trace-register' },
      },
      config: { url: '/auth/register', method: 'post' },
    }
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.mocked(authApi.register).mockRejectedValue(failure)
    const store = useAuthStore()

    await expect(store.register({
      username: 'nini',
      email: 'nini@example.com',
      password: 'UnitTest7!',
      displayName: 'Nini',
      code: '123456',
    })).rejects.toBe(failure)

    expect(consoleError).toHaveBeenCalledWith('[auth] register failed', {
      method: 'POST',
      url: '/auth/register',
      status: 404,
      code: 'NOT_FOUND',
      message: 'Not Found',
      traceId: 'trace-register',
    })
    consoleError.mockRestore()
  })

  it('clears local state even when server logout rejects the token', async () => {
    tokenStorage.write(tokens)
    vi.mocked(authApi.logout).mockRejectedValue(new Error('already revoked'))
    const store = useAuthStore()
    await store.restoreSession()
    store.tokens = tokens
    store.user = user
    store.status = 'authenticated'

    await store.logout()

    expect(authApi.logout).toHaveBeenCalledWith('refresh-token')
    expect(tokenStorage.read()).toBeNull()
    expect(store.status).toBe('anonymous')
    expect(store.user).toBeNull()
    expect(store.sessionExpired).toBe(false)
  })
})
