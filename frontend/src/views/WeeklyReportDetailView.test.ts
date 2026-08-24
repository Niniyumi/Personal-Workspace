import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WeeklyReportDetailView from './WeeklyReportDetailView.vue'

const mocks = vi.hoisted(() => ({
  loadOne: vi.fn(),
  update: vi.fn(),
  importDocx: vi.fn(),
  store: {
    current: null as null | Record<string, unknown>,
    imported: null,
    loading: false,
    error: null,
    loadOne: vi.fn(),
    update: vi.fn(),
    importDocx: vi.fn(),
  },
}))

vi.mock('../features/weeklyReport/weeklyReportStore', () => ({
  useWeeklyReportStore: () => mocks.store,
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return { ...actual, useRoute: () => ({ params: { id: '42' } }) }
})

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.current = null
  mocks.store.imported = null
  mocks.store.loading = false
  mocks.store.error = null
  mocks.store.loadOne = mocks.loadOne
  mocks.store.update = mocks.update
  mocks.store.importDocx = mocks.importDocx
})

describe('WeeklyReportDetailView', () => {
  it('loads the route report and updates its content', async () => {
    const report = {
      id: 42,
      weekStartDate: '2026-08-24',
      coreWork: '原来的内容',
      problems: null,
      nextWeekPlan: null,
      sourceFileName: null,
      createdAt: '2026-08-24T10:00:00',
      updatedAt: '2026-08-24T10:00:00',
    }
    mocks.loadOne.mockResolvedValue(report)
    mocks.update.mockResolvedValue({ ...report, coreWork: '更新后的内容' })
    const wrapper = mount(WeeklyReportDetailView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(mocks.loadOne).toHaveBeenCalledWith(42)
    await wrapper.get('[data-test="core-work"]').setValue('更新后的内容')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.update).toHaveBeenCalledWith(42, expect.objectContaining({
      coreWork: '更新后的内容',
    }))
  })
})
