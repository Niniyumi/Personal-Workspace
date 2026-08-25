import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from '../auth/authStore'
import { courseApi } from './courseApi'
import type { Course, CourseSummary } from './types'

export const useCourseStore = defineStore('course', () => {
  const courses = ref<CourseSummary[]>([])
  const current = ref<Course | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  let generation = 0

  function reset() {
    generation += 1
    courses.value = []
    current.value = null
    loading.value = false
    error.value = null
  }

  async function request<T>(operation: (token: string) => Promise<T>): Promise<T> {
    const auth = useAuthStore()
    const token = auth.tokens?.accessToken
    if (!token) throw new Error('登录状态已失效')
    error.value = null
    try {
      return await operation(token)
    } catch (cause) {
      if (isUnauthorized(cause)) {
        try {
          return await operation(await auth.refreshAccessToken())
        } catch (retryCause) {
          logCourseError(retryCause)
          error.value = '登录状态已失效，请重新登录'
          throw retryCause
        }
      }
      logCourseError(cause)
      error.value = '操作失败，请稍后重试'
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
    courses, current, loading, error, reset, loadAll, loadOne, get: loadOne,
    create, uploadPart, complete, retry, saveNote, downloadNote,
  }
})

function isUnauthorized(cause: unknown) {
  return typeof cause === 'object' && cause !== null
    && 'response' in cause
    && (cause as { response?: { status?: number } }).response?.status === 401
}

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
