import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WeeklyReportsView from './WeeklyReportsView.vue'

const mocks = vi.hoisted(() => ({
  loadMonth: vi.fn(),
  create: vi.fn(),
  importDocx: vi.fn(),
  recognizeDocx: vi.fn(),
  store: {
    reports: [] as Array<Record<string, unknown>>,
    imported: null,
    loading: false,
    error: null,
    loadMonth: vi.fn(),
    create: vi.fn(),
    importDocx: vi.fn(),
    recognizeDocx: vi.fn(),
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
  mocks.store.recognizeDocx = mocks.recognizeDocx
  mocks.loadMonth.mockResolvedValue(undefined)
})

describe('WeeklyReportsView', () => {
  it('makes the full report history entry a prominent button', () => {
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const link = wrapper.findAllComponents(RouterLinkStub)
      .find(candidate => {
        const to = candidate.props('to')
        return typeof to === 'object' && to?.name === 'weekly-report-history'
      })
    expect(link).toBeDefined()
    expect(link?.classes()).toContain('history-all-button')
    expect(link?.text()).toContain('按年份、内容查询全部周报')
  })

  it('uses large controls for filtering report history', () => {
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.get('[data-test="history-year"]').classes()).toContain('period-control')
    expect(wrapper.get('[data-test="history-month"]').classes()).toContain('period-control')
  })

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
    const detailLink = wrapper.findAllComponents(RouterLinkStub)
      .find(candidate => candidate.attributes('data-test') === 'report-detail-7')
    expect(detailLink?.props('to')).toEqual({
      name: 'weekly-report-detail',
      params: { id: 7 },
    })
    expect(detailLink?.classes()).toContain('action-link')
  })

  it('recognizes multiple files and merges files assigned to the same week', async () => {
    mocks.recognizeDocx
      .mockResolvedValueOnce({
        weekStartDate: '2026-09-07', coreWork: '完成登录', problems: null,
        nextWeekPlan: '准备联调', sourceFileName: '周报一.docx',
      })
      .mockResolvedValueOnce({
        weekStartDate: '2026-09-07', coreWork: '完成看板', problems: '接口较慢',
        nextWeekPlan: null, sourceFileName: '周报二.docx',
      })
    mocks.create.mockResolvedValue({ id: 8, weekStartDate: '2026-09-07' })
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    const input = wrapper.get('[data-test="weekly-docx-input"]')
    const files = [new File(['a'], '周报一.docx'), new File(['b'], '周报二.docx')]
    Object.defineProperty(input.element, 'files', { value: files })

    await input.trigger('change')
    await flushPromises()

    const batchItems = wrapper.findAll('[data-test="batch-report-item"]')
    expect(batchItems).toHaveLength(1)
    expect((batchItems[0]!.findAll('textarea')[0]!.element as HTMLTextAreaElement).value)
      .toBe('完成登录\n完成看板')
    expect(mocks.recognizeDocx).toHaveBeenNthCalledWith(1, files[0], { index: 1, total: 2 })
    expect(mocks.recognizeDocx).toHaveBeenNthCalledWith(2, files[1], { index: 2, total: 2 })
    await wrapper.get('[data-test="save-batch-reports"]').trigger('click')
    await flushPromises()

    expect(mocks.create).toHaveBeenCalledOnce()
    expect(mocks.create).toHaveBeenCalledWith(expect.objectContaining({
      weekStartDate: '2026-09-07',
      coreWork: '完成登录\n完成看板',
      sourceFileName: '周报一.docx、周报二.docx',
    }))
    expect(wrapper.get('[data-test="save-batch-reports"]').classes()).toContain('standard-action-button')
  })

  it('shows import progress and saves drafts from every preview page', async () => {
    const dates = ['2026-09-07', '2026-09-14', '2026-09-21', '2026-09-28', '2026-10-05', '2026-10-12']
    let releaseFirst!: (value: unknown) => void
    mocks.recognizeDocx.mockImplementationOnce(() => new Promise(resolve => { releaseFirst = resolve }))
    dates.slice(1).forEach((weekStartDate, index) => {
      mocks.recognizeDocx.mockResolvedValueOnce({
        weekStartDate, coreWork: `工作${index + 2}`, problems: null,
        nextWeekPlan: null, sourceFileName: `周报${index + 2}.docx`,
      })
    })
    mocks.create.mockResolvedValue({ id: 8 })
    const wrapper = mount(WeeklyReportsView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    const input = wrapper.get('[data-test="weekly-docx-input"]')
    Object.defineProperty(input.element, 'files', {
      value: dates.map((_, index) => new File(['a'], `周报${index + 1}.docx`)),
    })

    await input.trigger('change')
    expect(wrapper.find('.import-progress').text()).toContain('0/6')
    releaseFirst({ weekStartDate: dates[0], coreWork: '工作1', problems: null,
      nextWeekPlan: null, sourceFileName: '周报1.docx' })
    await flushPromises()

    expect(wrapper.findAll('[data-test="batch-report-item"]')).toHaveLength(5)
    await wrapper.get('[data-test="batch-next"]').trigger('click')
    expect(wrapper.findAll('[data-test="batch-report-item"]')).toHaveLength(1)
    await wrapper.get('[data-test="save-batch-reports"]').trigger('click')
    await flushPromises()

    expect(mocks.create).toHaveBeenCalledTimes(6)
    expect(wrapper.text()).toContain('成功导入 6 份')
    expect(document.querySelector('.el-message')?.textContent).toContain('成功导入 6 份')
  })

  it('shows recognition failure when no file produced a draft', async () => {
    mocks.recognizeDocx.mockRejectedValue(new Error('识别失败'))
    const wrapper = mount(WeeklyReportsView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    const input = wrapper.get('[data-test="weekly-docx-input"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['a'], '周报一.docx'), new File(['b'], '周报二.docx')],
    })

    await input.trigger('change')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('2 个文件识别失败')
  })

  it('shows history loading instead of an empty month', async () => {
    let finishLoad!: () => void
    mocks.loadMonth.mockReturnValue(new Promise<void>(resolve => { finishLoad = resolve }))
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-test="report-history-loading"]').text()).toContain('正在加载历史周报')
    expect(wrapper.text()).not.toContain('这个月份还没有周报')
    finishLoad()
    await flushPromises()
  })

  it('confirms when one DOCX has been recognized', async () => {
    mocks.importDocx.mockResolvedValue({
      weekStartDate: '2026-09-07', coreWork: '完成登录', problems: null,
      nextWeekPlan: null, sourceFileName: '周报.docx',
    })
    const wrapper = mount(WeeklyReportsView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    const input = wrapper.get('[data-test="weekly-docx-input"]')
    Object.defineProperty(input.element, 'files', { value: [new File(['a'], '周报.docx')] })

    await input.trigger('change')
    await flushPromises()

    expect(wrapper.get('[data-test="single-import-notice"]').text()).toContain('周报.docx 已识别')
  })
})
