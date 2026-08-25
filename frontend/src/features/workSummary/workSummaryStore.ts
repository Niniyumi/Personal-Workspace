import axios from 'axios'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logApiError } from '../../shared/logApiError'
import { useAuthStore } from '../auth/authStore'
import { workSummaryApi } from './workSummaryApi'
import type { WorkSummary, WorkSummaryInput, WorkSummarySelection } from './types'

export const useWorkSummaryStore = defineStore('workSummary', () => {
  const summary = ref<WorkSummary | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)

  function accessToken() {
    const token = useAuthStore().tokens?.accessToken
    if (!token) error.value = '登录状态已失效，请重新登录'
    return token || null
  }

  function errorMessage(cause: unknown) {
    if (axios.isAxiosError(cause)
        && (cause.response?.data as { code?: string } | undefined)?.code === 'NO_WEEKLY_REPORTS_FOR_PERIOD') {
      return '所选时间范围内还没有周报'
    }
    if (axios.isAxiosError(cause)
        && (cause.response?.data as { code?: string } | undefined)?.code === 'WORK_SUMMARY_NOT_FOUND') {
      return '还没有生成这个时间范围的总结'
    }
    return '操作失败，请稍后重试'
  }

  async function generate(selection: WorkSummarySelection): Promise<WorkSummary | null> {
    const token = accessToken()
    if (!token) return null
    loading.value = true
    error.value = null
    try {
      const result = await workSummaryApi.generate(token, selection)
      summary.value = result
      return result
    } catch (cause) {
      // 生成失败时保留上一份可见总结，避免用户正在编辑的内容消失。
      logApiError('work-summary', cause)
      error.value = errorMessage(cause)
      return null
    } finally {
      loading.value = false
    }
  }

  async function load(selection: WorkSummarySelection): Promise<WorkSummary | null> {
    const token = accessToken()
    if (!token) return null
    loading.value = true
    error.value = null
    try {
      const result = await workSummaryApi.get(token, selection)
      summary.value = result
      return result
    } catch (cause) {
      logApiError('work-summary', cause)
      error.value = errorMessage(cause)
      return null
    } finally {
      loading.value = false
    }
  }

  async function update(summaryId: number, input: WorkSummaryInput): Promise<WorkSummary | null> {
    const token = accessToken()
    if (!token) return null
    saving.value = true
    error.value = null
    try {
      const result = await workSummaryApi.update(token, summaryId, input)
      summary.value = result
      return result
    } catch (cause) {
      logApiError('work-summary', cause)
      error.value = errorMessage(cause)
      return null
    } finally {
      saving.value = false
    }
  }

  return { summary, loading, saving, error, generate, load, update }
})
