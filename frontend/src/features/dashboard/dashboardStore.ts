import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logApiError } from '../../shared/logApiError'
import { authenticatedRequest } from '../../shared/authenticatedRequest'
import { useAuthStore } from '../auth/authStore'
import { dashboardApi } from './dashboardApi'
import type { DashboardData } from './types'

export const useDashboardStore = defineStore('dashboard', () => {
  const data = ref<DashboardData | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      data.value = await authenticatedRequest((token) => dashboardApi.get(token))
    } catch (cause) {
      logApiError('dashboard', cause)
      error.value = useAuthStore().sessionExpired
        ? '登录状态已失效，请重新登录'
        : '看板加载失败，请稍后重试'
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, load }
})
