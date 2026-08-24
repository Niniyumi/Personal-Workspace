import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CoursesView from './CoursesView.vue'

const mocks = vi.hoisted(() => ({
  loadAll: vi.fn(),
  start: vi.fn(),
  store: {
    current: null as Record<string, unknown> | null,
    courses: [] as Array<Record<string, unknown>>,
    loading: false,
    error: null as string | null,
    reset: vi.fn(),
    loadAll: vi.fn(),
  },
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn() }) }
})

vi.mock('../features/course/courseStore', () => ({ useCourseStore: () => mocks.store }))
vi.mock('../features/course/useCourseRecorder', () => ({
  useCourseRecorder: () => ({
    status: ref('idle'),
    elapsedSeconds: ref(0),
    uploadedParts: ref(0),
    error: ref(null),
    canRetry: ref(false),
    isActive: ref(false),
    start: mocks.start,
    pause: vi.fn(),
    resume: vi.fn(),
    stop: vi.fn(),
    retryUploads: vi.fn(),
  }),
}))

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.courses = []
  mocks.store.loadAll = mocks.loadAll
  mocks.loadAll.mockResolvedValue(undefined)
  mocks.start.mockResolvedValue(undefined)
})

describe('CoursesView', () => {
  it('starts recording with the entered course title', async () => {
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await wrapper.get('[data-test="course-title"]').setValue('Java 并发课')
    await wrapper.get('[data-test="start-recording"]').trigger('click')

    expect(mocks.start).toHaveBeenCalledWith('Java 并发课')
  })

  it('loads and renders the current users course history', async () => {
    mocks.store.courses = [{
      id: 9,
      title: '数据库系统',
      status: 'READY',
      durationSeconds: 1800,
      createdAt: '2026-08-24T12:00:00Z',
    }]
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(mocks.loadAll).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('数据库系统')
    expect(wrapper.text()).toContain('已完成')
  })
})
