import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import WeeklyReportHistoryView from './WeeklyReportHistoryView.vue'

const mocks = vi.hoisted(() => ({
  search: vi.fn(),
  store: {
    searchResult: {
      items: [{ id: 9, weekStartDate: '2026-08-24', createdAt: '2026-08-25T08:00:00Z',
        characterCount: 120, sourceFileName: '周报.docx', preview: '完成登录模块' }],
      total: 11, page: 1,
    },
    error: null as string | null,
    search: vi.fn(),
  },
}))

vi.mock('../features/weeklyReport/weeklyReportStore', () => ({
  useWeeklyReportStore: () => mocks.store,
}))

describe('WeeklyReportHistoryView', () => {
  it('shows one report per row and queries the next page', async () => {
    mocks.store.search = mocks.search
    mocks.search.mockResolvedValue(undefined)
    const wrapper = mount(WeeklyReportHistoryView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(wrapper.findAll('[data-test="history-row"]')).toHaveLength(1)
    expect(wrapper.get('[data-test="history-row"]').text()).toContain('120')
    expect(wrapper.get('[data-test="history-row"]').text()).toContain('周报.docx')
    await wrapper.get('[data-test="history-next"]').trigger('click')
    await flushPromises()

    expect(mocks.search).toHaveBeenLastCalledWith(expect.any(Number), 0, '', 2)
  })
})
