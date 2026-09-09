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
})
