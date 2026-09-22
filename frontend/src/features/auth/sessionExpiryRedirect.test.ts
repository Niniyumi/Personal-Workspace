import { createPinia, setActivePinia } from 'pinia'
import { shallowRef } from 'vue'
import type { Router } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authApi'
import { useAuthStore } from './authStore'
import { installSessionExpiryRedirect } from './sessionExpiryRedirect'
import { tokenStorage } from './tokenStorage'

vi.mock('./authApi', () => ({
  authApi: { login: vi.fn(), currentUser: vi.fn(), refresh: vi.fn(), logout: vi.fn() },
}))

describe('session inactivity', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-22T10:00:00Z'))
    localStorage.clear()
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(authApi.login).mockResolvedValue({ accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 })
    vi.mocked(authApi.currentUser).mockResolvedValue({ id: 1, username: 'nini', email: 'nini@example.com', displayName: 'Nini' })
    vi.mocked(authApi.logout).mockResolvedValue(undefined)
  })

  afterEach(() => vi.useRealTimers())

  it('resets the fifteen-minute deadline after a user interaction', async () => {
    const auth = useAuthStore()
    await auth.login({ login: 'nini', password: 'UnitTest7!' })
    const replace = vi.fn()
    const router = { currentRoute: shallowRef({ name: 'home' }), replace } as unknown as Router
    const stop = installSessionExpiryRedirect(auth, router)

    vi.advanceTimersByTime(14 * 60 * 1000)
    window.dispatchEvent(new Event('pointerdown'))
    vi.advanceTimersByTime(14 * 60 * 1000)
    expect(auth.status).toBe('authenticated')
    vi.advanceTimersByTime(60 * 1000)

    expect(auth.status).toBe('anonymous')
    expect(tokenStorage.read()).toBeNull()
    expect(replace).toHaveBeenCalledWith({ name: 'login', query: { expired: '1' } })
    stop()
  })

  it('expires before accepting a click when background timers were paused', async () => {
    const auth = useAuthStore()
    await auth.login({ login: 'nini', password: 'UnitTest7!' })
    const replace = vi.fn()
    const router = { currentRoute: shallowRef({ name: 'home' }), replace } as unknown as Router
    const stop = installSessionExpiryRedirect(auth, router)

    vi.setSystemTime(new Date('2026-09-22T10:16:00Z'))
    window.dispatchEvent(new Event('pointerdown'))

    expect(auth.status).toBe('anonymous')
    expect(replace).toHaveBeenCalledWith({ name: 'login', query: { expired: '1' } })
    stop()
  })
})
