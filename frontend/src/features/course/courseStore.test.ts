import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../auth/authStore'
import { authApi } from '../auth/authApi'
import { courseApi } from './courseApi'
import { useCourseStore } from './courseStore'
import type { CourseSummary } from './types'

vi.mock('../auth/authApi', () => ({
  authApi: { refresh: vi.fn() },
}))

vi.mock('./courseApi', () => ({
  courseApi: { list: vi.fn(), uploadPart: vi.fn() },
}))

describe('courseStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('refreshes an expired access token and retries the course upload once', async () => {
    const auth = useAuthStore()
    auth.tokens = { accessToken: 'expired', refreshToken: 'refresh-token', expiresInSeconds: 900 }
    vi.mocked(authApi.refresh).mockResolvedValue({
      accessToken: 'fresh', refreshToken: 'new-refresh', expiresInSeconds: 900,
    })
    vi.mocked(courseApi.uploadPart)
      .mockRejectedValueOnce({ response: { status: 401 } })
      .mockResolvedValueOnce({ id: 1, partNumber: 1, durationSeconds: 300, fileSize: 5 })

    await useCourseStore().uploadPart(9, 1, 300, new Blob(['audio']))

    expect(authApi.refresh).toHaveBeenCalledWith('refresh-token')
    expect(courseApi.uploadPart).toHaveBeenNthCalledWith(2, 'fresh', 9, 1, 300, expect.any(Blob))
  })

  it('ignores a previous accounts delayed course response after reset', async () => {
    useAuthStore().tokens = { accessToken: 'user-a', refreshToken: 'refresh-a', expiresInSeconds: 900 }
    let resolveList: (value: CourseSummary[]) => void = () => undefined
    vi.mocked(courseApi.list).mockReturnValue(new Promise((resolve) => { resolveList = resolve }))
    const store = useCourseStore()

    const oldRequest = store.loadAll()
    store.reset()
    resolveList([{
      id: 1, title: '用户 A 的课程', status: 'READY', durationSeconds: 60,
      errorMessage: null, createdAt: '2026-08-24T12:00:00Z', updatedAt: '2026-08-24T12:00:00Z',
    }])
    await oldRequest

    expect(store.courses).toEqual([])
    expect(store.current).toBeNull()
  })
})
