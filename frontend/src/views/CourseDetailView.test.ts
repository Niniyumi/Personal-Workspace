import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CourseDetailView from './CourseDetailView.vue'

const mocks = vi.hoisted(() => ({
  loadOne: vi.fn(),
  saveNote: vi.fn(),
  retry: vi.fn(),
  store: {
    current: null as Record<string, unknown> | null,
    error: null as string | null,
    reset: vi.fn(),
    loadOne: vi.fn(),
    saveNote: vi.fn(),
    retry: vi.fn(),
  },
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return { ...actual, useRoute: () => ({ params: { id: '9' } }) }
})
vi.mock('../features/course/courseStore', () => ({ useCourseStore: () => mocks.store }))

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.loadOne = mocks.loadOne
  mocks.store.saveNote = mocks.saveNote
  mocks.store.retry = mocks.retry
  mocks.store.current = {
    id: 9,
    title: 'Java 并发课',
    status: 'READY',
    durationSeconds: 120,
    transcript: '完整转写',
    noteContent: '# 原笔记',
    errorMessage: null,
  }
  mocks.loadOne.mockResolvedValue(mocks.store.current)
  mocks.saveNote.mockResolvedValue(mocks.store.current)
  mocks.retry.mockResolvedValue(mocks.store.current)
})

describe('CourseDetailView', () => {
  it('shows the transcript and saves an edited note', async () => {
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.text()).toContain('完整转写')
    await wrapper.get('[data-test="course-note"]').setValue('# 新笔记')
    await wrapper.get('[data-test="save-course-note"]').trigger('click')

    expect(mocks.saveNote).toHaveBeenCalledWith(9, '# 新笔记')
  })

  it('offers a retry action after processing failure', async () => {
    mocks.store.current = { ...mocks.store.current, status: 'FAILED', errorMessage: '处理失败' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="retry-course"]').trigger('click')

    expect(mocks.retry).toHaveBeenCalledWith(9)
  })
})
