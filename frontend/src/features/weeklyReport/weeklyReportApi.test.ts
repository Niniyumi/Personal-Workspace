import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it } from 'vitest'
import { createWeeklyReportApi } from './weeklyReportApi'
import type { WeeklyReport, WeeklyReportInput } from './types'

const client = axios.create({ baseURL: '/api' })
const mock = new MockAdapter(client)
const api = createWeeklyReportApi(client)

const input: WeeklyReportInput = {
  weekStartDate: '2026-08-24',
  coreWork: '完成登录',
  problems: '接口超时',
  nextWeekPlan: '开发周报',
  sourceFileName: null,
}

const report: WeeklyReport = {
  id: 9,
  ...input,
  problems: input.problems,
  nextWeekPlan: input.nextWeekPlan,
  createdAt: '2026-08-24T08:00:00Z',
  updatedAt: '2026-08-24T10:00:00Z',
}

afterEach(() => mock.reset())

describe('weeklyReportApi', () => {
  it('searches one year with optional month, keyword and page', async () => {
    const result = { items: [], total: 0, page: 2 }
    mock.onGet('/weekly-reports/search', {
      params: { year: 2026, month: 8, keyword: '登录', page: 2 },
    }).reply(200, result)

    await expect(api.search('access-token', 2026, 8, '登录', 2)).resolves.toEqual(result)
    expect(mock.history.get[0]?.headers?.Authorization).toBe('Bearer access-token')
  })

  it('loads a month and one detail with bearer authentication', async () => {
    mock.onGet('/weekly-reports', { params: { year: 2026, month: 8 } }).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, [report]]
    })
    mock.onGet('/weekly-reports/9').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, report]
    })

    await expect(api.list('access-token', 2026, 8)).resolves.toEqual([report])
    await expect(api.get('access-token', 9)).resolves.toEqual(report)
  })

  it('creates and updates the exact three-field report contract', async () => {
    mock.onPost('/weekly-reports', input).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [201, report]
    })
    mock.onPut('/weekly-reports/9', input).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, report]
    })

    await expect(api.create('access-token', input)).resolves.toEqual(report)
    await expect(api.update('access-token', 9, input)).resolves.toEqual(report)
  })

  it('uploads a docx under the file multipart field', async () => {
    const file = new File(['docx'], 'week-34.docx')
    const classification = {
      weekStartDate: '2026-08-24',
      coreWork: '完成登录',
      problems: null,
      nextWeekPlan: '开发周报',
      sourceFileName: 'week-34.docx',
    }
    mock.onPost('/weekly-reports/import-docx').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      expect(config.data).toBeInstanceOf(FormData)
      expect((config.data as FormData).get('file')).toBe(file)
      return [200, classification]
    })

    await expect(api.importDocx('access-token', file, { index: 2, total: 5 }))
      .resolves.toEqual(classification)
    expect(mock.history.post[0]?.headers?.['X-Batch-Index']).toBe('2')
    expect(mock.history.post[0]?.headers?.['X-Batch-Total']).toBe('5')
  })
})
