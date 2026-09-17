import axios, { type AxiosInstance } from 'axios'
import type { Course, CourseAudioPart, CourseSummary } from './types'

function bearer(accessToken: string) {
  return { Authorization: `Bearer ${accessToken}` }
}

export function createCourseApi(client: AxiosInstance) {
  return {
    async createImport(accessToken: string, title: string, fileSize: number, fileName: string): Promise<Course> {
      const response = await client.post<Course>('/courses/imports', { title, fileSize, fileName }, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async importOffset(accessToken: string, courseId: number): Promise<number> {
      const response = await client.get<{ offset: number }>(`/courses/imports/${courseId}`, {
        headers: bearer(accessToken),
      })
      return response.data.offset
    },

    async uploadImportChunk(accessToken: string, courseId: number, offset: number, chunk: Blob): Promise<number> {
      const response = await client.put<{ offset: number }>(`/courses/imports/${courseId}/chunk?offset=${offset}`,
        chunk, { headers: { ...bearer(accessToken), 'Content-Type': 'application/octet-stream' },
          timeout: 120000 })
      return response.data.offset
    },

    async completeImport(accessToken: string, courseId: number): Promise<Course> {
      const response = await client.post<Course>(`/courses/imports/${courseId}/complete`, undefined, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async create(accessToken: string, title: string): Promise<Course> {
      const response = await client.post<Course>('/courses', { title }, { headers: bearer(accessToken) })
      return response.data
    },

    async list(accessToken: string): Promise<CourseSummary[]> {
      const response = await client.get<CourseSummary[]>('/courses', { headers: bearer(accessToken) })
      return response.data
    },

    async get(accessToken: string, courseId: number): Promise<Course> {
      const response = await client.get<Course>(`/courses/${courseId}`, { headers: bearer(accessToken) })
      return response.data
    },

    async uploadPart(
      accessToken: string,
      courseId: number,
      partNumber: number,
      durationSeconds: number,
      audio: Blob,
    ): Promise<CourseAudioPart> {
      const form = new FormData()
      form.append('partNumber', String(partNumber))
      form.append('durationSeconds', String(durationSeconds))
      form.append('file', audio, `part-${partNumber}.webm`)
      const response = await client.post<CourseAudioPart>(`/courses/${courseId}/parts`, form, {
        // 浏览器负责生成 multipart boundary，手写会导致后端无法解析文件。
        headers: bearer(accessToken),
      })
      return response.data
    },

    async complete(accessToken: string, courseId: number): Promise<Course> {
      const response = await client.post<Course>(`/courses/${courseId}/complete`, undefined, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async generateNote(accessToken: string, courseId: number): Promise<Course> {
      const response = await client.post<Course>(`/courses/${courseId}/note/generate`, undefined, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async listParts(accessToken: string, courseId: number): Promise<CourseAudioPart[]> {
      const response = await client.get<CourseAudioPart[]>(`/courses/${courseId}/parts`, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async readAudioPart(accessToken: string, courseId: number, partNumber: number): Promise<Blob> {
      const response = await client.get<Blob>(`/courses/${courseId}/parts/${partNumber}/audio`, {
        headers: bearer(accessToken),
        responseType: 'blob',
      })
      return response.data
    },

    async originalPlaybackUrl(accessToken: string, courseId: number): Promise<string> {
      const response = await client.post<{ url: string }>(`/courses/${courseId}/original/playback`, undefined, {
        headers: bearer(accessToken),
      })
      return response.data.url
    },

    async retry(accessToken: string, courseId: number): Promise<Course> {
      const response = await client.post<Course>(`/courses/${courseId}/retry`, undefined, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async saveNote(accessToken: string, courseId: number, noteContent: string): Promise<Course> {
      const response = await client.put<Course>(`/courses/${courseId}/note`, { noteContent }, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async downloadNote(accessToken: string, courseId: number): Promise<Blob> {
      const response = await client.get<Blob>(`/courses/${courseId}/note.docx`, {
        headers: bearer(accessToken),
        responseType: 'blob',
      })
      return response.data
    },
  }
}

export const courseApi = createCourseApi(axios.create({ baseURL: '/api' }))
