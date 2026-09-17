import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CoursesView from './CoursesView.vue'

const mocks = vi.hoisted(() => ({
  loadAll: vi.fn(),
  start: vi.fn(),
  uploadAudio: vi.fn(),
  store: {
    current: null as Record<string, unknown> | null,
    courses: [] as Array<Record<string, unknown>>,
    loading: false,
    error: null as string | null,
    reset: vi.fn(),
    loadAll: vi.fn(),
    uploadAudio: vi.fn(),
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
  mocks.store.uploadAudio = mocks.uploadAudio
  mocks.loadAll.mockResolvedValue(undefined)
  mocks.start.mockResolvedValue(undefined)
  mocks.uploadAudio.mockResolvedValue({ id: 9, status: 'PROCESSING' })
})

describe('CoursesView', () => {
  it('uploads a selected supported recording after entering the course title', async () => {
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    const file = new File(['audio'], 'lecture.m4a', { type: 'audio/mp4' })
    await wrapper.get('[data-test="course-title"]').setValue('网络课')
    const input = wrapper.get('[data-test="audio-file"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await wrapper.get('[data-test="upload-audio"]').trigger('click')
    expect(mocks.uploadAudio).toHaveBeenCalledWith('网络课', file, expect.any(Function))
  })

  it('shows why upload cannot start when the course title is empty', async () => {
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    const input = wrapper.get('[data-test="audio-file"]')
    Object.defineProperty(input.element, 'files', { value: [new File(['audio'], 'lecture.mp3')] })
    await input.trigger('change')
    await wrapper.get('[data-test="upload-audio"]').trigger('click')

    expect(wrapper.get('[role="alert"]').text()).toContain('请先填写课程名称')
    expect(mocks.uploadAudio).not.toHaveBeenCalled()
  })

  it('only offers M4A MP3 and WAV recording files', () => {
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })

    expect(wrapper.get('[data-test="audio-file"]').attributes('accept')).toBe('.m4a,.mp3,.wav')
    expect(wrapper.text()).not.toContain('M4A 文件')
    expect(wrapper.text()).not.toContain('上传 M4A')
  })

  it('asks for another recording when the selected file is invalid', async () => {
    mocks.uploadAudio.mockRejectedValue({ response: { data: { code: 'INVALID_AUDIO_FILE' } } })
    const wrapper = mount(CoursesView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await wrapper.get('[data-test="course-title"]').setValue('网络课')
    const input = wrapper.get('[data-test="audio-file"]')
    Object.defineProperty(input.element, 'files', { value: [new File(['bad'], 'lecture.m4a')] })
    await input.trigger('change')
    await wrapper.get('[data-test="upload-audio"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('请换一份录音文件')
  })
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
    expect(wrapper.get('.course-list a').classes()).toContain('action-link')
  })
})
