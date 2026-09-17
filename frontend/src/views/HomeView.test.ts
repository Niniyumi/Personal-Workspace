import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from './HomeView.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn(), logout: vi.fn(), load: vi.fn(),
  auth: { user: { displayName: 'Nini', email: 'nini@example.com' }, logout: vi.fn() },
  dashboard: {
    data: {
      totalWeeklyReports: 8, totalCourseNotes: 3, totalRecordingSeconds: 5400,
      monthWeeklyReports: 2, monthCourseNotes: 1, monthRecordingSeconds: 1800,
      monthlyActivity: [
        { month: '2026-08', weeklyReports: 2, courseNotes: 1 },
        { month: '2026-09', weeklyReports: 2, courseNotes: 1 },
      ],
      recentItems: [{ type: 'COURSE', id: 9, title: 'Java', updatedAt: '2026-09-08T04:00:00Z' }],
    },
    loading: false, error: null as string | null, load: vi.fn(),
  },
}))

vi.mock('../features/auth/authStore', () => ({ useAuthStore: () => mocks.auth }))
vi.mock('../features/dashboard/dashboardStore', () => ({ useDashboardStore: () => mocks.dashboard }))
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('vue-router')>()),
  useRouter: () => ({ push: mocks.push }),
}))

describe('HomeView dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.dashboard.load = mocks.load
    mocks.auth.logout = mocks.logout
  })

  it('shows real totals and removes development placeholders', async () => {
    mocks.load.mockResolvedValue(undefined)
    const wrapper = mount(HomeView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(mocks.load).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('8 份周报')
    expect(wrapper.text()).toContain('3 篇笔记')
    expect(wrapper.text()).toContain('90 分钟')
    expect(wrapper.text()).not.toContain('开发进度')
    expect(wrapper.text()).not.toContain('PHASE 01')
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.find('input[disabled]').exists()).toBe(false)
    expect(wrapper.get('[data-test="logout"]').classes()).toContain('standard-action-button')
    expect(wrapper.get('[data-test="open-weekly-reports"]').classes()).toContain('standard-action-button')
    expect(wrapper.get('.recent-item').classes()).toContain('interactive-row')
  })

  it('shows the dashboard retry as a visible standard action', async () => {
    mocks.dashboard.error = '看板加载失败'
    const wrapper = mount(HomeView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.get('[data-test="reload-dashboard"]').classes()).toContain('standard-action-button')
    mocks.dashboard.error = null
  })
})
