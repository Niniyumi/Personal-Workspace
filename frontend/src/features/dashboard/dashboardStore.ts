import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logApiError } from '../../shared/logApiError'
import { useAuthStore } from '../auth/authStore'
import { dashboardApi } from './dashboardApi'
import type { DashboardData } from './types'

export const useDashboardStore = defineStore('dashboard', () => {
  const data = ref<DashboardData | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(): Promise<void> {
    const accessToken = useAuthStore().tokens?.accessToken
    if (accessToken === undefined) {
      error.value = '登录状态已失效，请重新登录'
      return
    }

    loading.value = true
    error.value = null
    try {
      data.value = await dashboardApi.get(accessToken)
    } catch (cause) {
      logApiError('dashboard', cause)
      error.value = '看板加载失败，请稍后重试'
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, load }
})
