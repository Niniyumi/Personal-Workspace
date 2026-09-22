import { watch } from 'vue'
import type { Router } from 'vue-router'
import type { useAuthStore } from './authStore'
import { sessionActivity, SESSION_IDLE_MS } from './sessionActivity'

type AuthStore = ReturnType<typeof useAuthStore>

export function installSessionExpiryRedirect(auth: AuthStore, router: Router): () => void {
  const stopRedirect = watch(() => auth.sessionExpired, (expired) => {
    if (expired && router.currentRoute.value.name !== 'login') {
      void router.replace({ name: 'login', query: { expired: '1' } })
    }
  }, { flush: 'sync' })

  let timer: ReturnType<typeof setTimeout> | undefined
  function schedule(): void {
    if (timer !== undefined) clearTimeout(timer)
    if (auth.status !== 'authenticated') return
    const lastAt = sessionActivity.lastAt() ?? Date.now()
    const remaining = SESSION_IDLE_MS - (Date.now() - lastAt)
    if (remaining <= 0) {
      auth.expireInactiveSession()
      return
    }
    timer = setTimeout(schedule, remaining)
  }

  function onActivity(): void {
    if (auth.status !== 'authenticated') return
    if (sessionActivity.isInactive()) {
      auth.expireInactiveSession()
      return
    }
    sessionActivity.touch()
    schedule()
  }

  function onVisibilityChange(): void {
    if (document.visibilityState === 'visible') {
      if (sessionActivity.isInactive()) auth.expireInactiveSession()
      else schedule()
    }
  }

  const stopStatus = watch(() => auth.status, schedule, { immediate: true, flush: 'sync' })
  for (const event of ['pointerdown', 'keydown', 'wheel', 'touchstart']) {
    window.addEventListener(event, onActivity, { passive: true, capture: true })
  }
  document.addEventListener('visibilitychange', onVisibilityChange)

  return () => {
    stopRedirect()
    stopStatus()
    if (timer !== undefined) clearTimeout(timer)
    for (const event of ['pointerdown', 'keydown', 'wheel', 'touchstart']) {
      window.removeEventListener(event, onActivity, true)
    }
    document.removeEventListener('visibilitychange', onVisibilityChange)
  }
}
