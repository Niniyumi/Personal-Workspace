import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authenticatedRequest } from '../../shared/authenticatedRequest'
import { useAuthStore } from '../auth/authStore'
import { courseApi } from './courseApi'
import type { Course, CourseAudioPart, CourseSummary } from './types'

export const useCourseStore = defineStore('course', () => {
  const courses = ref<CourseSummary[]>([])
  const current = ref<Course | null>(null)
  const parts = ref<CourseAudioPart[]>([])
  const audioUrls = ref<Record<number, string>>({})
  const loading = ref(false)
  const error = ref<string | null>(null)
  let generation = 0

  function reset() {
    generation += 1
    courses.value = []
    current.value = null
    Object.values(audioUrls.value).forEach((url) => URL.revokeObjectURL(url))
    audioUrls.value = {}
    parts.value = []
    loading.value = false
    error.value = null
  }

  async function request<T>(operation: (token: string) => Promise<T>): Promise<T> {
    const auth = useAuthStore()
    error.value = null
    try {
      return await authenticatedRequest(operation)
    } catch (cause) {
      logCourseError(cause)
      error.value = auth.sessionExpired ? '登录状态已失效，请重新登录' : '操作失败，请稍后重试'
      throw cause
    }
  }

  async function loadAll() {
    const requestGeneration = generation
    loading.value = true
    courses.value = []
    try {
      const loaded = await request((token) => courseApi.list(token))
      if (requestGeneration === generation) courses.value = loaded
    } finally {
      if (requestGeneration === generation) loading.value = false
    }
  }

  async function loadOne(courseId: number) {
    const requestGeneration = generation
    current.value = null
    const loaded = await request((token) => courseApi.get(token, courseId))
    if (requestGeneration === generation) current.value = loaded
    return loaded
  }

  async function create(title: string) {
    const requestGeneration = generation
    const course = await request((token) => courseApi.create(token, title))
    if (requestGeneration === generation) {
      current.value = course
      courses.value = [course, ...courses.value]
    }
    return course
  }

  async function uploadPart(courseId: number, partNumber: number, duration: number, audio: Blob) {
    await request((token) => courseApi.uploadPart(token, courseId, partNumber, duration, audio))
  }

  async function complete(courseId: number) {
    const requestGeneration = generation
    const course = await request((token) => courseApi.complete(token, courseId))
    if (requestGeneration === generation) current.value = course
    return course
  }

  async function uploadAudio(title: string, file: File, onProgress: (percent: number) => void) {
    if (!title.trim() || !/\.(m4a|mp3|wav)$/i.test(file.name)
        || file.size < 1 || file.size > 512 * 1024 * 1024) {
      error.value = '请选择不超过 512 MB 的 M4A、MP3 或 WAV 文件，并填写课程名称'
      throw new Error(error.value)
    }
    const auth = useAuthStore()
    const key = `course-import:${auth.user?.id ?? 'user'}:${file.name}:${file.size}`
    const remembered = Number(localStorage.getItem(key))
    const created = remembered > 0 ? null
      : await request(token => courseApi.createImport(token, title, file.size, file.name))
    const courseId = created?.id ?? remembered
    if (created) localStorage.setItem(key, String(courseId))
    let offset = await request(token => courseApi.importOffset(token, courseId))
    while (offset < file.size) {
      const chunk = file.slice(offset, Math.min(offset + 8 * 1024 * 1024, file.size))
      const saved = await request(token => courseApi.uploadImportChunk(token, courseId, offset, chunk))
      if (saved <= offset || saved > file.size) throw new Error('上传进度异常，请重试')
      offset = saved
      onProgress(Math.round(offset / file.size * 100))
    }
    const course = await request(token => courseApi.completeImport(token, courseId))
    localStorage.removeItem(key)
    current.value = course
    courses.value = [course, ...courses.value.filter(item => item.id !== course.id)]
    return course
  }

  async function generateNote(courseId: number) {
    const requestGeneration = generation
    const course = await request((token) => courseApi.generateNote(token, courseId))
    if (requestGeneration === generation) current.value = course
    return course
  }

  async function loadParts(courseId: number) {
    const loaded = await request((token) => courseApi.listParts(token, courseId))
    parts.value = loaded
    return loaded
  }

  async function loadAudioPart(courseId: number, partNumber: number) {
    if (audioUrls.value[partNumber]) return audioUrls.value[partNumber]
    const blob = await request((token) => courseApi.readAudioPart(token, courseId, partNumber))
    const url = URL.createObjectURL(blob)
    audioUrls.value = { ...audioUrls.value, [partNumber]: url }
    return url
  }

  async function loadOriginalAudio(courseId: number) {
    return request(token => courseApi.originalPlaybackUrl(token, courseId))
  }

  async function retry(courseId: number) {
    const requestGeneration = generation
    const course = await request((token) => courseApi.retry(token, courseId))
    if (requestGeneration === generation) current.value = course
    return course
  }

  async function saveNote(courseId: number, noteContent: string) {
    const requestGeneration = generation
    const course = await request((token) => courseApi.saveNote(token, courseId, noteContent))
    if (requestGeneration === generation) current.value = course
    return course
  }

  async function downloadNote(courseId: number, title: string) {
    const blob = await request((token) => courseApi.downloadNote(token, courseId))
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${title}.docx`
    link.click()
    URL.revokeObjectURL(url)
  }

  return {
    courses, current, parts, audioUrls, loading, error, reset, loadAll, loadOne, get: loadOne,
    create, uploadAudio, uploadPart, complete, generateNote, loadParts, loadAudioPart, loadOriginalAudio,
    retry, saveNote, downloadNote,
  }
})

// 记录接口定位信息，不记录音频内容、访问令牌等敏感数据。
function logCourseError(cause: unknown) {
  const requestError = cause as {
    config?: { method?: string; url?: string }
    response?: { status?: number; data?: { code?: string; traceId?: string } }
  }
  console.error('[course] request failed', {
    method: requestError.config?.method?.toUpperCase(),
    url: requestError.config?.url,
    status: requestError.response?.status,
    code: requestError.response?.data?.code,
    traceId: requestError.response?.data?.traceId,
  })
}
