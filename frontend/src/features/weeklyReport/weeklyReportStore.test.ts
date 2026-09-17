import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../auth/authStore'
import { weeklyReportApi } from './weeklyReportApi'
import { useWeeklyReportStore } from './weeklyReportStore'
import type { DocxImportResult, WeeklyReport } from './types'

vi.mock('./weeklyReportApi', () => ({
  weeklyReportApi: {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    importDocx: vi.fn(),
  },
}))

const report: WeeklyReport = {
  id: 9,
  weekStartDate: '2026-08-24',
  coreWork: '完成登录',
  problems: null,
  nextWeekPlan: '开发周报',
  sourceFileName: null,
  createdAt: '2026-08-24T08:00:00Z',
  updatedAt: '2026-08-24T10:00:00Z',
}

describe('weeklyReportStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
    useAuthStore().tokens = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    }
  })

  it('loads reports for the selected month', async () => {
    vi.mocked(weeklyReportApi.list).mockResolvedValue([report])
    const store = useWeeklyReportStore()

    await store.loadMonth(2026, 8)

    expect(weeklyReportApi.list).toHaveBeenCalledWith('access-token', 2026, 8)
    expect(store.reports).toEqual([report])
  })

  it('keeps the imported classification ready for the form', async () => {
    const imported: DocxImportResult = {
      weekStartDate: '2026-08-24',
      coreWork: '完成登录',
      problems: null,
      nextWeekPlan: '开发周报',
      sourceFileName: 'week-34.docx',
    }
    vi.mocked(weeklyReportApi.importDocx).mockResolvedValue(imported)
    const store = useWeeklyReportStore()
    const file = new File(['docx'], 'week-34.docx')

    await store.importDocx(file)

    expect(weeklyReportApi.importDocx).toHaveBeenCalledWith(
      'access-token',
      file,
      { index: 1, total: 1 },
    )
    expect(store.imported).toEqual(imported)
  })

  it('does not call the api when the login token is missing', async () => {
    const auth = useAuthStore()
    auth.tokens = null
    const store = useWeeklyReportStore()

    await store.loadMonth(2026, 8)

    expect(weeklyReportApi.list).not.toHaveBeenCalled()
    expect(store.error).toBe('登录状态已失效，请重新登录')
    expect(auth.sessionExpired).toBe(true)
  })

  it('refreshes an expired token and retries the weekly report request once', async () => {
    const auth = useAuthStore()
    vi.spyOn(auth, 'refreshAccessToken').mockResolvedValue('fresh-token')
    vi.mocked(weeklyReportApi.list)
      .mockRejectedValueOnce({ response: { status: 401 } })
      .mockResolvedValueOnce([report])

    await useWeeklyReportStore().loadMonth(2026, 8)

    expect(auth.refreshAccessToken).toHaveBeenCalledOnce()
    expect(weeklyReportApi.list).toHaveBeenNthCalledWith(2, 'fresh-token', 2026, 8)
  })

  it('records request failure and always ends the loading state', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.mocked(weeklyReportApi.list).mockRejectedValue(new Error('network failed'))
    const store = useWeeklyReportStore()

    await expect(store.loadMonth(2026, 8)).rejects.toThrow('network failed')

    expect(store.loading).toBe(false)
    expect(store.error).toBe('操作失败，请稍后重试')
    expect(consoleError).toHaveBeenCalledWith('[weekly-report] request failed', expect.any(Object))
  })
})
