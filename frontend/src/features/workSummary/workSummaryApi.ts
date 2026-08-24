import axios, { type AxiosInstance } from 'axios'
import type { WorkSummary, WorkSummaryInput, WorkSummarySelection } from './types'

function bearer(accessToken: string) {
  return { Authorization: `Bearer ${accessToken}` }
}

export function createWorkSummaryApi(client: AxiosInstance) {
  return {
    async generate(accessToken: string, selection: WorkSummarySelection): Promise<WorkSummary> {
      const response = await client.post<WorkSummary>('/work-summaries/generate', selection, {
        headers: bearer(accessToken),
      })
      return response.data
    },

    async get(accessToken: string, selection: WorkSummarySelection): Promise<WorkSummary> {
      const response = await client.get<WorkSummary>('/work-summaries', {
        params: selection,
        headers: bearer(accessToken),
      })
      return response.data
    },

    async update(
      accessToken: string,
      summaryId: number,
      input: WorkSummaryInput,
    ): Promise<WorkSummary> {
      const response = await client.put<WorkSummary>(`/work-summaries/${summaryId}`, input, {
        headers: bearer(accessToken),
      })
      return response.data
    },
  }
}

export const workSummaryApi = createWorkSummaryApi(axios.create({ baseURL: '/api' }))
