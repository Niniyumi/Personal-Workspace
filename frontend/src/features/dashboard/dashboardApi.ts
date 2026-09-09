import axios, { type AxiosInstance } from 'axios'
import type { DashboardData } from './types'

export function createDashboardApi(client: AxiosInstance) {
  return {
    async get(accessToken: string): Promise<DashboardData> {
      const response = await client.get<DashboardData>('/dashboard', {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      return response.data
    },
  }
}

export const dashboardApi = createDashboardApi(axios.create({ baseURL: '/api' }))
