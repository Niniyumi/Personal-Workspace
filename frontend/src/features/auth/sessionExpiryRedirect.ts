import { watch } from 'vue'
import type { Router } from 'vue-router'
import type { useAuthStore } from './authStore'

type AuthStore = ReturnType<typeof useAuthStore>

export function installSessionExpiryRedirect(auth: AuthStore, router: Router): () => void {
  return watch(() => auth.sessionExpired, (expired) => {
    if (expired && router.currentRoute.value.name !== 'login') {
      void router.replace({ name: 'login', query: { expired: '1' } })
    }
  }, { flush: 'sync' })
}
