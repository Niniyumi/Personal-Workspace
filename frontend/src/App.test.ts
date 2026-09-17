import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import { useAuthStore } from './features/auth/authStore'
import { installSessionExpiryRedirect } from './features/auth/sessionExpiryRedirect'
import { createAppRouter } from './router'

describe('App session expiry navigation', () => {
  it('moves an expired active session to login with a clear reason', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.status = 'authenticated'
    const router = createAppRouter(createMemoryHistory())
    await router.push('/courses')
    const replace = vi.spyOn(router, 'replace')
    const stopRedirect = installSessionExpiryRedirect(auth, router)
    const redirected = new Promise<void>((resolve) => {
      router.afterEach((to) => {
        if (to.name === 'login') resolve()
      })
    })

    auth.expireSession()
    await redirected

    expect(auth.sessionExpired).toBe(true)
    expect(replace).toHaveBeenCalledWith({ name: 'login', query: { expired: '1' } })
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.expired).toBe('1')
    expect(router.currentRoute.value.query.redirect).toBeUndefined()
    stopRedirect()
  })
})
