import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it } from 'vitest'
import { createCourseApi } from './courseApi'
import type { Course } from './types'

const client = axios.create({ baseURL: '/api' })
const mock = new MockAdapter(client)
const api = createCourseApi(client)

const course: Course = {
  id: 9,
  title: 'Java 并发课',
  status: 'RECORDING',
  durationSeconds: 0,
  processingProgress: 0,
  transcript: null,
  noteContent: null,
  errorMessage: null,
  createdAt: '2026-08-24T12:00:00Z',
  updatedAt: '2026-08-24T12:00:00Z',
}

afterEach(() => mock.reset())

describe('courseApi', () => {
  it('creates, lists and loads a course with bearer authentication', async () => {
    mock.onPost('/courses', { title: 'Java 并发课' }).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [201, course]
    })
    mock.onGet('/courses').reply(200, [course])
    mock.onGet('/courses/9').reply(200, course)

    await expect(api.create('access-token', 'Java 并发课')).resolves.toEqual(course)
    await expect(api.list('access-token')).resolves.toEqual([course])
    await expect(api.get('access-token', 9)).resolves.toEqual(course)
  })

  it('uploads one audio part with the exact multipart fields', async () => {
    const audio = new Blob(['audio'], { type: 'audio/webm' })
    mock.onPost('/courses/9/parts').reply((config) => {
      const form = config.data as FormData
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      expect(form.get('partNumber')).toBe('2')
      expect(form.get('durationSeconds')).toBe('120')
      expect(form.get('file')).toBeInstanceOf(Blob)
      return [200, { id: 3, partNumber: 2, durationSeconds: 120, fileSize: 5 }]
    })

    await expect(api.uploadPart('access-token', 9, 2, 120, audio)).resolves.toMatchObject({ partNumber: 2 })
  })

  it('completes, retries and saves notes', async () => {
    mock.onPost('/courses/9/complete').reply(202, { ...course, status: 'PROCESSING' })
    mock.onPost('/courses/9/retry').reply(202, { ...course, status: 'PROCESSING' })
    mock.onPut('/courses/9/note', { noteContent: '# 新笔记' }).reply(200, {
      ...course,
      status: 'READY',
      noteContent: '# 新笔记',
    })

    await expect(api.complete('access-token', 9)).resolves.toMatchObject({ status: 'PROCESSING' })
    await expect(api.retry('access-token', 9)).resolves.toMatchObject({ status: 'PROCESSING' })
    await expect(api.saveNote('access-token', 9, '# 新笔记')).resolves.toMatchObject({ noteContent: '# 新笔记' })
  })

  it('downloads the generated note as a docx blob', async () => {
    mock.onGet('/courses/9/note.docx').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      expect(config.responseType).toBe('blob')
      return [200, new Blob(['docx'])]
    })

    await expect(api.downloadNote('access-token', 9)).resolves.toBeInstanceOf(Blob)
  })
})
