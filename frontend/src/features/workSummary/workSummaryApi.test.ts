import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it } from 'vitest'
import { createWorkSummaryApi } from './workSummaryApi'
import type { WorkSummary } from './types'

const client = axios.create({ baseURL: '/api' })
const mock = new MockAdapter(client)
const api = createWorkSummaryApi(client)

const summary: WorkSummary = {
  id: 7,
  periodType: 'QUARTER',
  periodStart: '2026-07-01',
  periodEnd: '2026-09-30',
  coreContent: '核心成果',
  routineWork: '日常维护',
  selfScore: 88,
  generatedAt: '2026-08-24T10:00:00Z',
  createdAt: '2026-08-24T10:00:00Z',
  updatedAt: '2026-08-24T10:00:00Z',
}

afterEach(() => mock.reset())

describe('workSummaryApi', () => {
  it('generates and loads a quarter with bearer authentication', async () => {
    const input = { periodType: 'QUARTER' as const, year: 2026, quarter: 3 }
    mock.onPost('/work-summaries/generate', input).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, summary]
    })
    mock.onGet('/work-summaries', { params: input }).reply(200, summary)

    await expect(api.generate('access-token', input)).resolves.toEqual(summary)
    await expect(api.get('access-token', input)).resolves.toEqual(summary)
  })

  it('updates only editable summary content', async () => {
    const input = { coreContent: '新核心', routineWork: '新日常', selfScore: 92 }
    mock.onPut('/work-summaries/7', input).reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, { ...summary, ...input }]
    })

    await expect(api.update('access-token', 7, input)).resolves.toMatchObject(input)
  })
})
