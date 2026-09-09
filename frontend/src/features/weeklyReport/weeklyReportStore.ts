import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logApiError } from '../../shared/logApiError'
import { useAuthStore } from '../auth/authStore'
import { weeklyReportApi } from './weeklyReportApi'
import type { BatchProgress } from './weeklyReportApi'
import type { DocxImportResult, WeeklyReport, WeeklyReportInput } from './types'

export const useWeeklyReportStore = defineStore('weeklyReport', () => {
  const reports = ref<WeeklyReport[]>([])
  const current = ref<WeeklyReport | null>(null)
  const imported = ref<DocxImportResult | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function request<T>(operation: (accessToken: string) => Promise<T>): Promise<T | null> {
    loading.value = true
    error.value = null
    const accessToken = useAuthStore().tokens?.accessToken
    if (accessToken === undefined) {
      error.value = '登录状态已失效，请重新登录'
      loading.value = false
      return null
    }
    try {
      return await operation(accessToken)
    } catch (cause) {
      logApiError('weekly-report', cause)
      error.value = '操作失败，请稍后重试'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function loadMonth(year: number, month: number): Promise<void> {
    const loaded = await request((token) => weeklyReportApi.list(token, year, month))
    if (loaded !== null) reports.value = loaded
  }

  async function loadOne(reportId: number): Promise<WeeklyReport | null> {
    const loaded = await request((token) => weeklyReportApi.get(token, reportId))
    if (loaded !== null) current.value = loaded
    return loaded
  }

  async function create(input: WeeklyReportInput): Promise<WeeklyReport | null> {
    const saved = await request((token) => weeklyReportApi.create(token, input))
    if (saved !== null) current.value = saved
    return saved
  }

  async function update(reportId: number, input: WeeklyReportInput): Promise<WeeklyReport | null> {
    const saved = await request((token) => weeklyReportApi.update(token, reportId, input))
    if (saved !== null) current.value = saved
    return saved
  }

  async function importDocx(file: File): Promise<DocxImportResult | null> {
    const result = await recognizeDocx(file, { index: 1, total: 1 })
    if (result !== null) imported.value = result
    return result
  }

  async function recognizeDocx(file: File, progress: BatchProgress): Promise<DocxImportResult | null> {
    return request((token) => weeklyReportApi.importDocx(token, file, progress))
  }

  return {
    reports,
    current,
    imported,
    loading,
    error,
    loadMonth,
    loadOne,
    create,
    update,
    importDocx,
    recognizeDocx,
  }
})
