import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { describe, expect, it } from 'vitest'
import { createDashboardApi } from './dashboardApi'

describe('dashboardApi', () => {
  it('loads the authenticated dashboard', async () => {
    const client = axios.create()
    const mock = new MockAdapter(client)
    const response = {
      totalWeeklyReports: 4, totalCourseNotes: 2, totalRecordingSeconds: 900,
      monthWeeklyReports: 1, monthCourseNotes: 1, monthRecordingSeconds: 600,
      monthlyActivity: [], recentItems: [],
    }
    mock.onGet('/dashboard').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, response]
    })

    await expect(createDashboardApi(client).get('access-token')).resolves.toEqual(response)
  })
})
