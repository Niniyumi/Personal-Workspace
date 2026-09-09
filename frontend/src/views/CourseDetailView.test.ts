import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CourseDetailView from './CourseDetailView.vue'

const mocks = vi.hoisted(() => ({
  loadOne: vi.fn(),
  saveNote: vi.fn(),
  retry: vi.fn(),
  downloadNote: vi.fn(),
  generateNote: vi.fn(),
  loadParts: vi.fn(),
  loadAudioPart: vi.fn(),
  store: {
    current: null as Record<string, unknown> | null,
    error: null as string | null,
    reset: vi.fn(),
    loadOne: vi.fn(),
    saveNote: vi.fn(),
    retry: vi.fn(),
    downloadNote: vi.fn(),
    generateNote: vi.fn(),
    loadParts: vi.fn(),
    loadAudioPart: vi.fn(),
    parts: [{ id: 1, partNumber: 1, durationSeconds: 120, fileSize: 5 }],
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
  mocks.store.downloadNote = mocks.downloadNote
  mocks.store.generateNote = mocks.generateNote
  mocks.store.loadParts = mocks.loadParts
  mocks.store.loadAudioPart = mocks.loadAudioPart
  mocks.store.current = {
    id: 9,
    title: 'Java 并发课',
    status: 'READY',
    durationSeconds: 120,
    processingProgress: 100,
    transcript: '完整转写',
    noteContent: '# 原笔记',
    errorMessage: null,
  }
  mocks.loadOne.mockResolvedValue(mocks.store.current)
  mocks.saveNote.mockResolvedValue(mocks.store.current)
  mocks.retry.mockResolvedValue(mocks.store.current)
  mocks.downloadNote.mockResolvedValue(undefined)
  mocks.generateNote.mockResolvedValue(mocks.store.current)
  mocks.loadParts.mockResolvedValue(mocks.store.parts)
  mocks.loadAudioPart.mockResolvedValue('blob:audio')
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

  it('shows the real processing progress', async () => {
    mocks.store.current = { ...mocks.store.current, status: 'PROCESSING', processingProgress: 65 }
    mocks.loadOne.mockResolvedValue(mocks.store.current)

    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.get('[data-test="course-progress"]').text()).toContain('65%')
  })

  it('downloads a completed saved note', async () => {
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="download-course-note"]').trigger('click')

    expect(mocks.downloadNote).toHaveBeenCalledWith(9, 'Java 并发课')
  })

  it('lets the user generate notes only after transcription is saved', async () => {
    mocks.store.current = { ...mocks.store.current, status: 'TRANSCRIBED', noteContent: null }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.text()).toContain('完整转写')
    await wrapper.get('[data-test="generate-course-note"]').trigger('click')

    expect(mocks.generateNote).toHaveBeenCalledWith(9)
  })

  it('keeps original audio available for playback', async () => {
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="load-course-audio-1"]').trigger('click')
    await flushPromises()

    expect(mocks.loadAudioPart).toHaveBeenCalledWith(9, 1)
    expect(wrapper.get('audio').attributes('src')).toBe('blob:audio')
  })

  it('shows note generation failure without hiding the saved transcript', async () => {
    mocks.store.current = {
      ...mocks.store.current,
      status: 'TRANSCRIBED',
      noteContent: null,
      errorMessage: '笔记生成失败，请重试',
    }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.text()).toContain('完整转写')
    expect(wrapper.get('[data-test="note-generation-error"]').text()).toContain('笔记生成失败，请重试')
  })
})
