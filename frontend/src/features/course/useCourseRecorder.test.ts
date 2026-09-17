import { describe, expect, it, vi } from 'vitest'
import { useCourseRecorder, type CourseRecorderStore, type RecorderLike } from './useCourseRecorder'

describe('useCourseRecorder', () => {
  it('reports starting while waiting for microphone permission and ignores a duplicate start', async () => {
    let allowMicrophone: ((stream: MediaStream) => void) | undefined
    const getUserMedia = vi.fn(() => new Promise<MediaStream>((resolve) => { allowMicrophone = resolve }))
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart: vi.fn(),
      complete: vi.fn(),
    }
    const controls = useCourseRecorder(store, {
      getUserMedia,
      createRecorder: () => new FakeRecorder(),
      setInterval: vi.fn(() => 1),
      clearInterval: vi.fn(),
    })

    const starting = controls.start('课程')
    expect(controls.status.value).toBe('starting')
    await controls.start('重复课程')
    expect(getUserMedia).toHaveBeenCalledOnce()

    allowMicrophone?.({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream)
    await starting
    expect(controls.status.value).toBe('recording')
  })

  it('uploads the final segment before completing the course', async () => {
    const calls: string[] = []
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart: vi.fn(async (_id, partNumber, duration) => {
        calls.push(`upload:${partNumber}:${duration}`)
      }),
      complete: vi.fn(async () => {
        calls.push('complete')
      }),
    }
    let tick: () => void = () => undefined
    let recorder: FakeRecorder | undefined
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream),
      createRecorder: () => {
        recorder = new FakeRecorder()
        return recorder
      },
      setInterval: (callback) => {
        tick = callback
        return 1
      },
      clearInterval: vi.fn(),
    })

    await controls.start('Java 并发课')
    tick()
    tick()
    await controls.stop()

    expect(recorder?.started).toBe(true)
    expect(calls).toEqual(['upload:1:2', 'complete'])
    expect(controls.status.value).toBe('processing')
  })

  it('does not create a course when microphone permission fails', async () => {
    const store: CourseRecorderStore = {
      create: vi.fn(),
      uploadPart: vi.fn(),
      complete: vi.fn(),
    }
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => { throw new Error('denied') },
      createRecorder: () => new FakeRecorder(),
      setInterval: vi.fn(),
      clearInterval: vi.fn(),
    })

    await expect(controls.start('课程')).rejects.toThrow('denied')

    expect(store.create).not.toHaveBeenCalled()
    expect(controls.error.value).toBe('无法使用麦克风，请检查浏览器权限')
  })

  it('stops once at 9000 seconds and completes after the thirtieth segment', async () => {
    const uploaded: number[] = []
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart: vi.fn(async (_id, partNumber) => { uploaded.push(partNumber) }),
      complete: vi.fn(async () => undefined),
    }
    let tick: () => void = () => undefined
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream),
      createRecorder: () => new FakeRecorder(),
      setInterval: (callback) => { tick = callback; return 1 },
      clearInterval: vi.fn(),
    })

    await controls.start('操作系统')
    for (let second = 1; second <= 9000; second += 1) {
      tick()
      if (second % 300 === 0) await vi.waitFor(() => expect(uploaded.length).toBe(second / 300))
    }

    await vi.waitFor(() => expect(controls.status.value).toBe('processing'))
    expect(uploaded).toEqual(Array.from({ length: 30 }, (_, index) => index + 1))
    expect(store.complete).toHaveBeenCalledOnce()
  })

  it('stops the microphone after an upload failure and can retry the failed part', async () => {
    const trackStop = vi.fn()
    const uploadPart = vi.fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValue(undefined)
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart,
      complete: vi.fn(async () => undefined),
    }
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: trackStop }] } as unknown as MediaStream),
      createRecorder: () => new FakeRecorder(),
      setInterval: vi.fn(() => 1),
      clearInterval: vi.fn(),
    })

    await controls.start('数据库')
    await expect(controls.stop()).rejects.toThrow('offline')

    expect(trackStop).toHaveBeenCalled()
    expect(controls.canRetry.value).toBe(true)
    await controls.retryUploads()
    expect(uploadPart).toHaveBeenCalledTimes(2)
    expect(store.complete).toHaveBeenCalledOnce()
    expect(controls.status.value).toBe('processing')
  })

  it('keeps the new tail segment when a rotating upload fails', async () => {
    const uploadPart = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(undefined)
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart,
      complete: vi.fn(async () => undefined),
    }
    let tick: () => void = () => undefined
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream),
      createRecorder: () => new FakeRecorder(),
      setInterval: (callback) => { tick = callback; return 1 },
      clearInterval: vi.fn(),
    })

    await controls.start('计算机网络')
    for (let second = 0; second < 300; second += 1) tick()
    await vi.waitFor(() => expect(controls.status.value).toBe('error'))
    await controls.retryUploads()

    expect(uploadPart.mock.calls.map((call) => call[1])).toEqual([1, 1, 2])
    expect(store.complete).toHaveBeenCalledOnce()
  })

  it('can retry when audio uploaded but the complete request failed', async () => {
    const complete = vi.fn().mockRejectedValueOnce(new Error('timeout')).mockResolvedValue(undefined)
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      uploadPart: vi.fn(async () => undefined),
      complete,
    }
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream),
      createRecorder: () => new FakeRecorder(),
      setInterval: vi.fn(() => 1),
      clearInterval: vi.fn(),
    })

    await controls.start('编译原理')
    await expect(controls.stop()).rejects.toThrow('timeout')
    expect(controls.canRetry.value).toBe(true)
    expect(controls.error.value).toBe('课程处理启动失败，请重试')

    await controls.retryUploads()
    expect(store.uploadPart).toHaveBeenCalledTimes(2)
    expect(complete).toHaveBeenCalledTimes(2)
    expect(controls.status.value).toBe('processing')
  })

  it('does not reupload when the complete response was lost after processing started', async () => {
    const store: CourseRecorderStore = {
      create: vi.fn(async () => ({ id: 9 })),
      get: vi.fn(async () => ({ status: 'PROCESSING' })),
      uploadPart: vi.fn(async () => undefined),
      complete: vi.fn().mockRejectedValueOnce(new Error('response lost')),
    }
    const controls = useCourseRecorder(store, {
      getUserMedia: async () => ({ getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream),
      createRecorder: () => new FakeRecorder(),
      setInterval: vi.fn(() => 1),
      clearInterval: vi.fn(),
    })

    await controls.start('软件工程')
    await expect(controls.stop()).rejects.toThrow('response lost')
    await controls.retryUploads()

    expect(store.uploadPart).toHaveBeenCalledOnce()
    expect(store.complete).toHaveBeenCalledOnce()
    expect(controls.status.value).toBe('processing')
  })
})

class FakeRecorder implements RecorderLike {
  state = 'inactive'
  started = false
  ondataavailable: ((event: BlobEvent) => unknown) | null = null
  onstop: ((event: Event) => unknown) | null = null

  start() {
    this.started = true
    this.state = 'recording'
  }

  stop() {
    this.state = 'inactive'
    this.ondataavailable?.({ data: new Blob(['audio'], { type: 'audio/webm' }) } as BlobEvent)
    this.onstop?.(new Event('stop'))
  }

  pause() { this.state = 'paused' }
  resume() { this.state = 'recording' }
}
