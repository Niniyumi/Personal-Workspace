import axios from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../auth/authStore'
import { workSummaryApi } from './workSummaryApi'
import { useWorkSummaryStore } from './workSummaryStore'

vi.mock('./workSummaryApi', () => ({
  workSummaryApi: { generate: vi.fn(), get: vi.fn(), update: vi.fn() },
}))

describe('workSummaryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    useAuthStore().tokens = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    }
  })

  it('keeps the previous summary and explains an empty period', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    const store = useWorkSummaryStore()
    store.summary = {
      id: 7,
      periodType: 'YEAR',
      periodStart: '2025-01-01',
      periodEnd: '2025-12-31',
      coreContent: '旧总结',
      routineWork: '旧日常',
      selfScore: 80,
      generatedAt: '',
      createdAt: '',
      updatedAt: '',
    }
    vi.mocked(workSummaryApi.generate).mockRejectedValue(new axios.AxiosError(
      'conflict', '409', undefined, undefined,
      { data: { code: 'NO_WEEKLY_REPORTS_FOR_PERIOD' }, status: 409, statusText: '', headers: {}, config: {} as never },
    ))

    await store.generate({ periodType: 'YEAR', year: 2026 })

    expect(store.summary.coreContent).toBe('旧总结')
    expect(store.error).toBe('所选时间范围内还没有周报')
    expect(store.loading).toBe(false)
    expect(consoleError).toHaveBeenCalledWith('[work-summary] request failed', expect.any(Object))
  })

  it('updates the visible summary and saving state', async () => {
    const updated = {
      id: 7,
      periodType: 'YEAR' as const,
      periodStart: '2026-01-01',
      periodEnd: '2026-12-31',
      coreContent: '新总结',
      routineWork: '新日常',
      selfScore: 90,
      generatedAt: '',
      createdAt: '',
      updatedAt: '',
    }
    vi.mocked(workSummaryApi.update).mockResolvedValue(updated)
    const store = useWorkSummaryStore()

    await store.update(7, { coreContent: '新总结', routineWork: '新日常', selfScore: 90 })

    expect(store.summary).toEqual(updated)
    expect(store.saving).toBe(false)
  })

  it('refreshes an expired token and retries loading a summary once', async () => {
    const auth = useAuthStore()
    vi.spyOn(auth, 'refreshAccessToken').mockResolvedValue('fresh-token')
    const loaded = {
      id: 7, periodType: 'YEAR' as const, periodStart: '2026-01-01', periodEnd: '2026-12-31',
      coreContent: '总结', routineWork: '日常', selfScore: 90,
      generatedAt: '', createdAt: '', updatedAt: '',
    }
    vi.mocked(workSummaryApi.get)
      .mockRejectedValueOnce({ response: { status: 401 } })
      .mockResolvedValueOnce(loaded)

    await useWorkSummaryStore().load({ periodType: 'YEAR', year: 2026 })

    expect(auth.refreshAccessToken).toHaveBeenCalledOnce()
    expect(workSummaryApi.get).toHaveBeenNthCalledWith(2, 'fresh-token', { periodType: 'YEAR', year: 2026 })
  })
})
