import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useDashboardStore } from './dashboardStore'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  auth: { tokens: { accessToken: 'access-token' }, refreshAccessToken: vi.fn() },
}))

vi.mock('../auth/authStore', () => ({ useAuthStore: () => mocks.auth }))
vi.mock('./dashboardApi', () => ({ dashboardApi: { get: mocks.get } }))

describe('dashboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mocks.auth.tokens = { accessToken: 'access-token' }
  })

  it('keeps real dashboard data after loading', async () => {
    const response = {
      totalWeeklyReports: 4, totalCourseNotes: 2, totalRecordingSeconds: 900,
      monthWeeklyReports: 1, monthCourseNotes: 1, monthRecordingSeconds: 600,
      monthlyActivity: [], recentItems: [],
    }
    mocks.get.mockResolvedValue(response)

    const store = useDashboardStore()
    await store.load()

    expect(store.data).toEqual(response)
    expect(store.error).toBeNull()
  })

  it('refreshes an expired token and retries loading the dashboard once', async () => {
    mocks.get.mockRejectedValueOnce({ response: { status: 401 } }).mockResolvedValueOnce({
      totalWeeklyReports: 0, totalCourseNotes: 0, totalRecordingSeconds: 0,
      monthWeeklyReports: 0, monthCourseNotes: 0, monthRecordingSeconds: 0,
      monthlyActivity: [], recentItems: [],
    })
    mocks.auth.refreshAccessToken.mockResolvedValue('fresh-token')

    await useDashboardStore().load()

    expect(mocks.auth.refreshAccessToken).toHaveBeenCalledOnce()
    expect(mocks.get).toHaveBeenNthCalledWith(2, 'fresh-token')
  })
})
