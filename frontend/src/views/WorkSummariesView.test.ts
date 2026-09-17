import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkSummariesView from './WorkSummariesView.vue'

const mocks = vi.hoisted(() => ({
  generate: vi.fn(),
  load: vi.fn(),
  update: vi.fn(),
  store: {
    summary: null as null | Record<string, unknown>,
    loading: false,
    saving: false,
    error: null as string | null,
    generate: vi.fn(),
    load: vi.fn(),
    update: vi.fn(),
  },
}))

vi.mock('../features/workSummary/workSummaryStore', () => ({
  useWorkSummaryStore: () => mocks.store,
}))

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.summary = null
  mocks.store.loading = false
  mocks.store.saving = false
  mocks.store.error = null
  mocks.store.generate = mocks.generate
  mocks.store.load = mocks.load
  mocks.store.update = mocks.update
  mocks.generate.mockResolvedValue(null)
  mocks.load.mockResolvedValue(null)
})

describe('WorkSummariesView', () => {
  it('uses large controls for selecting the summary period', () => {
    const wrapper = mount(WorkSummariesView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.get('[data-test="summary-type-quarter"]').classes()).toContain('period-control')
    expect(wrapper.get('[data-test="summary-type-year"]').classes()).toContain('period-control')
    expect(wrapper.get('[data-test="summary-year"]').classes()).toContain('period-control')
    expect(wrapper.get('[data-test="summary-quarter"]').classes()).toContain('period-control')
  })

  it('switches modes and requires a quarter in quarter mode', async () => {
    const wrapper = mount(WorkSummariesView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('[data-test="summary-quarter"]').exists()).toBe(true)
    await wrapper.get('[data-test="summary-type-year"]').trigger('click')
    expect(wrapper.find('[data-test="summary-quarter"]').exists()).toBe(false)
  })

  it('generates the selected quarter once', async () => {
    const wrapper = mount(WorkSummariesView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await wrapper.get('[data-test="summary-year"]').setValue('2026')
    await wrapper.get('[data-test="summary-quarter"]').setValue('3')
    await wrapper.get('[data-test="generate-summary"]').trigger('click')
    await flushPromises()

    expect(mocks.generate).toHaveBeenCalledOnce()
    expect(mocks.generate).toHaveBeenCalledWith({ periodType: 'QUARTER', year: 2026, quarter: 3 })
  })

  it('renders and saves the three editable modules', async () => {
    mocks.store.summary = {
      id: 7,
      periodType: 'YEAR',
      periodStart: '2026-01-01',
      periodEnd: '2026-12-31',
      coreContent: '核心成果',
      routineWork: '日常维护',
      selfScore: 88,
      generatedAt: '',
      createdAt: '',
      updatedAt: '',
    }
    mocks.update.mockResolvedValue(undefined)
    const wrapper = mount(WorkSummariesView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.text()).toContain('核心内容')
    expect(wrapper.text()).toContain('日常工作')
    expect(wrapper.text()).toContain('自我评分')
    expect(wrapper.get('[data-test="summary-score"]').attributes()).toMatchObject({ min: '0', max: '100' })
    await wrapper.get('[data-test="summary-core"]').setValue('修改后的核心')
    await wrapper.get('[data-test="save-summary"]').trigger('click')
    await flushPromises()

    expect(mocks.update).toHaveBeenCalledWith(7, expect.objectContaining({ coreContent: '修改后的核心' }))
    expect(wrapper.get('[data-test="save-summary"]').classes()).toContain('standard-action-button')
  })

  it('shows loading only on the summary action that was clicked', async () => {
    let finishLoad!: (value: unknown) => void
    mocks.load.mockReturnValue(new Promise(resolve => { finishLoad = resolve }))
    const wrapper = mount(WorkSummariesView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    await wrapper.get('[data-test="load-saved-summary"]').trigger('click')

    expect(wrapper.get('[data-test="load-saved-summary"]').classes()).toContain('is-loading')
    expect(wrapper.get('[data-test="generate-summary"]').classes()).not.toContain('is-loading')
    finishLoad(null)
    await flushPromises()
  })
})
