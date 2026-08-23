import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore'
import { createAppRouter } from './index'

describe('authentication router guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('restores the session before the first protected navigation', async () => {
    const store = useAuthStore()
    const restore = vi.spyOn(store, 'restoreSession').mockImplementation(async () => {
      store.status = 'authenticated'
    })
    const router = createAppRouter(createMemoryHistory())

    await router.push('/')

    expect(restore).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('home')
  })

  it('redirects an anonymous user to login and keeps the target path', async () => {
    const store = useAuthStore()
    vi.spyOn(store, 'restoreSession').mockImplementation(async () => {
      store.status = 'anonymous'
    })
    const router = createAppRouter(createMemoryHistory())

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/')
  })

  it('redirects an authenticated user away from guest pages', async () => {
    const store = useAuthStore()
    store.status = 'authenticated'
    const router = createAppRouter(createMemoryHistory())

    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('home')
  })

  it('redirects an unknown URL to home', async () => {
    const store = useAuthStore()
    store.status = 'authenticated'
    const router = createAppRouter(createMemoryHistory())

    await router.push('/missing')

    expect(router.currentRoute.value.path).toBe('/')
  })
})
