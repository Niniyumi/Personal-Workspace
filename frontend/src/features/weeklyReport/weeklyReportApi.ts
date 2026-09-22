import axios, { type AxiosInstance } from 'axios'
import type { DocxImportResult, WeeklyReport, WeeklyReportInput, WeeklyReportSearchResult } from './types'

export interface BatchProgress {
  index: number
  total: number
}

function bearer(accessToken: string) {
  return { Authorization: `Bearer ${accessToken}` }
}

export function createWeeklyReportApi(client: AxiosInstance) {
  return {
    async search(accessToken: string, year: number, month: number, keyword: string, page: number): Promise<WeeklyReportSearchResult> {
      const response = await client.get<WeeklyReportSearchResult>('/weekly-reports/search', {
        params: { year, ...(month ? { month } : {}), keyword, page },
        headers: bearer(accessToken),
      })
      return response.data
    },

    async list(accessToken: string, year: number, month: number): Promise<WeeklyReport[]> {
      const response = await client.get<WeeklyReport[]>('/weekly-reports', {
        params: { year, month },
        headers: bearer(accessToken),
      })
      return response.data
    },

    async get(accessToken: string, reportId: number): Promise<WeeklyReport> {
      const response = await client.get<WeeklyReport>(`/weekly-reports/${reportId}`, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async create(accessToken: string, input: WeeklyReportInput): Promise<WeeklyReport> {
      const response = await client.post<WeeklyReport>('/weekly-reports', input, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async update(
      accessToken: string,
      reportId: number,
      input: WeeklyReportInput,
    ): Promise<WeeklyReport> {
      const response = await client.put<WeeklyReport>(`/weekly-reports/${reportId}`, input, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async importDocx(
      accessToken: string,
      file: File,
      progress: BatchProgress = { index: 1, total: 1 },
    ): Promise<DocxImportResult> {
      const formData = new FormData()
      formData.append('file', file)
      const response = await client.post<DocxImportResult>('/weekly-reports/import-docx', formData, {
        // 不手写 multipart boundary，由浏览器根据 FormData 自动生成。
        headers: {
          ...bearer(accessToken),
          'X-Batch-Index': String(progress.index),
          'X-Batch-Total': String(progress.total),
        },
      })
      return response.data
    },
  }
}

export const weeklyReportApi = createWeeklyReportApi(axios.create({ baseURL: '/api' }))
