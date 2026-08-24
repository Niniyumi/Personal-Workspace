import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WeeklyReportsView from './WeeklyReportsView.vue'

const mocks = vi.hoisted(() => ({
  loadMonth: vi.fn(),
  create: vi.fn(),
  importDocx: vi.fn(),
  store: {
    reports: [] as Array<Record<string, unknown>>,
    imported: null,
    loading: false,
    error: null,
    loadMonth: vi.fn(),
    create: vi.fn(),
    importDocx: vi.fn(),
  },
}))

vi.mock('../features/weeklyReport/weeklyReportStore', () => ({
  useWeeklyReportStore: () => mocks.store,
}))

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.reports = []
  mocks.store.imported = null
  mocks.store.loading = false
  mocks.store.error = null
  mocks.store.loadMonth = mocks.loadMonth
  mocks.store.create = mocks.create
  mocks.store.importDocx = mocks.importDocx
  mocks.loadMonth.mockResolvedValue(undefined)
})

describe('WeeklyReportsView', () => {
  it('loads reports again after changing the year and month', async () => {
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()
    mocks.loadMonth.mockClear()

    await wrapper.get('[data-test="history-year"]').setValue('2025')
    await wrapper.get('[data-test="history-month"]').setValue('6')
    await flushPromises()

    expect(mocks.loadMonth).toHaveBeenLastCalledWith(2025, 6)
  })

  it('renders the report date, preview and detail link', () => {
    mocks.store.reports = [{
      id: 7,
      weekStartDate: '2026-08-24',
      coreWork: '完成周报模块的数据接口与联调',
      problems: null,
      nextWeekPlan: null,
      sourceFileName: null,
      createdAt: '2026-08-24T10:00:00',
      updatedAt: '2026-08-24T10:00:00',
    }]

    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.text()).toContain('2026-08-24')
    expect(wrapper.text()).toContain('完成周报模块的数据接口与联调')
    const detailLink = wrapper.findAllComponents(RouterLinkStub)[1]
    expect(detailLink?.props('to')).toEqual({
      name: 'weekly-report-detail',
      params: { id: 7 },
    })
  })
})
