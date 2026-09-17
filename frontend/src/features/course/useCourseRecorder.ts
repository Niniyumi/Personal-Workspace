import { computed, ref } from 'vue'

export interface CourseRecorderStore {
  create(title: string): Promise<{ id: number } | null>
  get?(courseId: number): Promise<{ status: string } | null>
  uploadPart(courseId: number, partNumber: number, duration: number, audio: Blob): Promise<unknown>
  complete(courseId: number): Promise<unknown>
}

export interface RecorderLike {
  state: string
  ondataavailable: ((event: BlobEvent) => unknown) | null
  onstop: ((event: Event) => unknown) | null
  start(): void
  stop(): void
  pause(): void
  resume(): void
}

interface RecorderDependencies {
  getUserMedia: () => Promise<MediaStream>
  createRecorder: (stream: MediaStream) => RecorderLike
  setInterval: (callback: () => void) => number
  clearInterval: (timer: number) => void
}

type RecorderStatus = 'idle' | 'starting' | 'recording' | 'paused' | 'uploading' | 'processing' | 'error'
type PendingPart = { partNumber: number; duration: number; audio: Blob }

const SEGMENT_SECONDS = 300
const MAX_SECONDS = 9000

function browserDependencies(): RecorderDependencies {
  return {
    getUserMedia: () => navigator.mediaDevices.getUserMedia({ audio: true }),
    createRecorder: (stream) => new MediaRecorder(stream, { mimeType: 'audio/webm' }),
    setInterval: (callback) => window.setInterval(callback, 1000),
    clearInterval: (timer) => window.clearInterval(timer),
  }
}

export function useCourseRecorder(
  store: CourseRecorderStore,
  dependencies: RecorderDependencies = browserDependencies(),
) {
  const status = ref<RecorderStatus>('idle')
  const elapsedSeconds = ref(0)
  const uploadedParts = ref(0)
  const error = ref<string | null>(null)
  const canRetry = ref(false)
  const isActive = computed(() => status.value === 'recording' || status.value === 'paused')

  let courseId: number | null = null
  let stream: MediaStream | null = null
  let recorder: RecorderLike | null = null
  let timer: number | null = null
  let segmentSeconds = 0
  let nextPartNumber = 1
  let chunks: Blob[] = []
  let uploadQueue = Promise.resolve()
  let rotationPromise: Promise<void> | null = null
  let failedParts: PendingPart[] = []
  let lastUploadedPart: PendingPart | null = null

  function startSegment() {
    if (!stream) return
    chunks = []
    segmentSeconds = 0
    recorder = dependencies.createRecorder(stream)
    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) chunks.push(event.data)
    }
    recorder.start()
  }

  function finishSegment(restart: boolean): Promise<void> {
    const current = recorder
    if (!current || current.state === 'inactive') return Promise.resolve()
    const duration = Math.max(1, segmentSeconds)
    return new Promise((resolve, reject) => {
      current.onstop = () => {
        const audio = new Blob(chunks, { type: 'audio/webm' })
        const partNumber = nextPartNumber++
        const pendingPart = { partNumber, duration, audio }
        if (restart && status.value === 'recording') startSegment()
        uploadQueue = uploadQueue
          .then(async () => {
            if (courseId === null) return
            await store.uploadPart(courseId, partNumber, duration, audio)
            lastUploadedPart = pendingPart
            uploadedParts.value += 1
          })
          .catch((cause) => {
            failedParts.push(pendingPart)
            throw cause
          })
          .then(resolve, reject)
      }
      current.stop()
    })
  }

  async function rotateSegment() {
    if (rotationPromise !== null || status.value !== 'recording') return
    rotationPromise = finishSegment(true)
    try {
      await rotationPromise
    } catch (cause) {
      await preserveCurrentSegment()
      failRecording('音频分片上传失败，请重试上传')
      throw cause
    } finally {
      rotationPromise = null
    }
  }

  function preserveCurrentSegment(): Promise<void> {
    const current = recorder
    if (!current || current.state === 'inactive') return Promise.resolve()
    const duration = Math.max(1, segmentSeconds)
    return new Promise((resolve) => {
      current.onstop = () => {
        const audio = new Blob(chunks, { type: 'audio/webm' })
        failedParts.push({ partNumber: nextPartNumber++, duration, audio })
        resolve()
      }
      current.stop()
    })
  }

  function closeMedia() {
    if (recorder && recorder.state !== 'inactive') {
      recorder.ondataavailable = null
      recorder.onstop = null
      recorder.stop()
    }
    stream?.getTracks().forEach((track) => track.stop())
    stream = null
    recorder = null
    if (timer !== null) dependencies.clearInterval(timer)
    timer = null
  }

  function failRecording(message: string) {
    closeMedia()
    canRetry.value = failedParts.length > 0
    status.value = 'error'
    error.value = message
  }

  async function start(title: string) {
    if (status.value !== 'idle' && status.value !== 'error') return
    status.value = 'starting'
    error.value = null
    canRetry.value = false
    failedParts = []
    lastUploadedPart = null
    uploadQueue = Promise.resolve()
    try {
      stream = await dependencies.getUserMedia()
    } catch (cause) {
      status.value = 'error'
      error.value = '无法使用麦克风，请检查浏览器权限'
      throw cause
    }
    try {
      const course = await store.create(title.trim())
      if (!course) throw new Error('course create failed')
      courseId = course.id
      elapsedSeconds.value = 0
      uploadedParts.value = 0
      nextPartNumber = 1
      status.value = 'recording'
      startSegment()
      timer = dependencies.setInterval(() => {
        if (status.value !== 'recording') return
        elapsedSeconds.value += 1
        segmentSeconds += 1
        if (elapsedSeconds.value >= MAX_SECONDS) {
          void stop().catch(() => undefined)
          return
        }
        if (segmentSeconds >= SEGMENT_SECONDS) void rotateSegment().catch(() => undefined)
      })
    } catch (cause) {
      stream.getTracks().forEach((track) => track.stop())
      stream = null
      status.value = 'error'
      error.value = '课程创建失败，请稍后重试'
      throw cause
    }
  }

  function pause() {
    if (recorder?.state !== 'recording') return
    recorder.pause()
    status.value = 'paused'
  }

  function resume() {
    if (recorder?.state !== 'paused') return
    recorder.resume()
    status.value = 'recording'
  }

  async function stop() {
    if (courseId === null || !isActive.value) return
    status.value = 'uploading'
    if (timer !== null) dependencies.clearInterval(timer)
    let completing = false
    try {
      if (rotationPromise !== null) await rotationPromise
      await finishSegment(false)
      await uploadQueue
      completing = true
      await store.complete(courseId)
      closeMedia()
      status.value = 'processing'
    } catch (cause) {
      if (failedParts.length === 0 && lastUploadedPart !== null) {
        failedParts = [lastUploadedPart]
      }
      failRecording(completing ? '课程处理启动失败，请重试' : '音频上传失败，请重试上传')
      throw cause
    }
  }

  async function retryUploads() {
    if (courseId === null || failedParts.length === 0) return
    status.value = 'uploading'
    error.value = null
    const pending = [...failedParts]
    failedParts = []
    uploadQueue = Promise.resolve()
    try {
      const remote = await store.get?.(courseId)
      if (remote && remote.status !== 'RECORDING') {
        canRetry.value = false
        status.value = 'processing'
        return
      }
      for (let index = 0; index < pending.length; index += 1) {
        const part = pending[index]
        try {
          await store.uploadPart(courseId, part.partNumber, part.duration, part.audio)
          uploadedParts.value += 1
        } catch (cause) {
          failedParts = pending.slice(index)
          throw cause
        }
      }
      await store.complete(courseId)
      canRetry.value = false
      status.value = 'processing'
    } catch (cause) {
      if (failedParts.length === 0 && pending.length > 0) {
        // complete 请求失败时重复上传最后一片是安全的，服务端会按分片号去重。
        failedParts = [pending[pending.length - 1]!]
      }
      canRetry.value = true
      status.value = 'error'
      error.value = '重试失败，请检查网络后再次尝试'
      throw cause
    }
  }

  return { status, elapsedSeconds, uploadedParts, error, canRetry, isActive, start, pause, resume, stop, retryUploads }
}
