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
  courseApi: { list: vi.fn(), uploadPart: vi.fn(), createImport: vi.fn(), importOffset: vi.fn(),
    uploadImportChunk: vi.fn(), completeImport: vi.fn(), get: vi.fn(), getStatus: vi.fn(), rename: vi.fn() },
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

  it.each([
    ['lecture.m4a', 'audio/mp4'],
    ['lecture.mp3', 'audio/mpeg'],
    ['lecture.wav', 'audio/wav'],
  ])('uploads supported recording %s in chunks', async (fileName, contentType) => {
    useAuthStore().tokens = { accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 }
    const imported = { id: 9, title: '网络课', status: 'UPLOADING' as const,
      durationSeconds: 0, processingProgress: 0, transcript: null, noteContent: null,
      noteCandidate: null,
      errorMessage: null, sourceType: 'IMPORT' as const, createdAt: '', updatedAt: '' }
    vi.mocked(courseApi.createImport).mockResolvedValue(imported)
    vi.mocked(courseApi.importOffset).mockResolvedValue(0)
    vi.mocked(courseApi.uploadImportChunk).mockImplementation(async (_token, _id, offset, blob) => offset + blob.size)
    vi.mocked(courseApi.completeImport).mockResolvedValue({ ...imported, status: 'PROCESSING' })
    const file = new File([new Uint8Array(8 * 1024 * 1024 + 3)], fileName, { type: contentType })
    const progress: number[] = []

    const result = await useCourseStore().uploadAudio('网络课', file, value => progress.push(value))

    expect(result.status).toBe('PROCESSING')
    expect(courseApi.createImport).toHaveBeenCalledWith('access', '网络课', file.size, fileName, undefined)
    expect(vi.mocked(courseApi.uploadImportChunk).mock.calls.map(call => call[2])).toEqual([0, 8 * 1024 * 1024])
    expect(progress.at(-1)).toBe(100)
    expect(courseApi.completeImport).toHaveBeenCalledOnce()
  })

  it('rejects unsupported recording formats before creating an import', async () => {
    useAuthStore().tokens = { accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 }
    const file = new File(['audio'], 'lecture.aac', { type: 'audio/aac' })

    await expect(useCourseStore().uploadAudio('网络课', file, () => undefined))
      .rejects.toThrow('M4A、MP3 或 WAV')
    expect(courseApi.createImport).not.toHaveBeenCalled()
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
      processingProgress: 100,
      errorMessage: null, createdAt: '2026-08-24T12:00:00Z', updatedAt: '2026-08-24T12:00:00Z',
    }])
    await oldRequest

    expect(store.courses).toEqual([])
    expect(store.current).toBeNull()
  })

  it('logs request details when a course request fails', async () => {
    useAuthStore().tokens = { accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 }
    const failure = {
      config: { method: 'get', url: '/courses' },
      response: { status: 404, data: { code: 'NOT_FOUND', traceId: 'course-trace' } },
    }
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.mocked(courseApi.list).mockRejectedValue(failure)

    await expect(useCourseStore().loadAll()).rejects.toBe(failure)

    expect(consoleError).toHaveBeenCalledWith('[course] request failed', {
      method: 'GET', url: '/courses', status: 404, code: 'NOT_FOUND', traceId: 'course-trace',
    })
    consoleError.mockRestore()
  })

  it('merges lightweight progress without clearing the loaded transcript', async () => {
    useAuthStore().tokens = { accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 }
    const course = {
      id: 9, title: '网络课', status: 'PROCESSING' as const, durationSeconds: 300,
      processingProgress: 10, transcript: '已完成的转写', noteContent: null, noteCandidate: null,
      errorMessage: null, sourceType: 'IMPORT' as const, createdAt: '', updatedAt: '',
    }
    vi.mocked(courseApi.get).mockResolvedValue(course)
    vi.mocked(courseApi.getStatus).mockResolvedValue({
      status: 'PROCESSING', processingProgress: 65, errorMessage: null,
    })

    const store = useCourseStore()
    await store.loadOne(9)
    await store.refreshStatus(9)
    expect(store.current).toMatchObject({ transcript: '已完成的转写', processingProgress: 65 })
  })

  it('updates the current course and its list item after renaming', async () => {
    useAuthStore().tokens = { accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 }
    const course = {
      id: 9, title: '原课程名', status: 'READY' as const, durationSeconds: 300,
      processingProgress: 100, transcript: '转写', noteContent: '笔记', noteCandidate: null,
      errorMessage: null, sourceType: 'IMPORT' as const, createdAt: '', updatedAt: '',
    }
    vi.mocked(courseApi.get).mockResolvedValue(course)
    vi.mocked(courseApi.list).mockResolvedValue([course])
    vi.mocked(courseApi.rename).mockResolvedValue({ title: '新笔记名称' })
    const store = useCourseStore()
    await Promise.all([store.loadOne(9), store.loadAll()])

    await store.rename(9, '新笔记名称')

    expect(store.current?.title).toBe('新笔记名称')
    expect(store.current?.transcript).toBe('转写')
    expect(store.courses[0]?.title).toBe('新笔记名称')
  })
})
