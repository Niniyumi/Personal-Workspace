import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CourseDetailView from './CourseDetailView.vue'

const mocks = vi.hoisted(() => ({
  loadOne: vi.fn(),
  saveNote: vi.fn(),
  retry: vi.fn(),
  downloadNote: vi.fn(),
  generateNote: vi.fn(),
  resolveNoteCandidate: vi.fn(),
  loadParts: vi.fn(),
  loadAudioPart: vi.fn(),
  loadOriginalAudio: vi.fn(),
  store: {
    current: null as Record<string, unknown> | null,
    error: null as string | null,
    reset: vi.fn(),
    loadOne: vi.fn(),
    saveNote: vi.fn(),
    retry: vi.fn(),
    downloadNote: vi.fn(),
    generateNote: vi.fn(),
    resolveNoteCandidate: vi.fn(),
    loadParts: vi.fn(),
    loadAudioPart: vi.fn(),
    loadOriginalAudio: vi.fn(),
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
  mocks.store.parts = [{ id: 1, partNumber: 1, durationSeconds: 120, fileSize: 5 }]
  mocks.store.loadOne = mocks.loadOne
  mocks.store.saveNote = mocks.saveNote
  mocks.store.retry = mocks.retry
  mocks.store.downloadNote = mocks.downloadNote
  mocks.store.generateNote = mocks.generateNote
  mocks.store.resolveNoteCandidate = mocks.resolveNoteCandidate
  mocks.store.loadParts = mocks.loadParts
  mocks.store.loadAudioPart = mocks.loadAudioPart
  mocks.store.loadOriginalAudio = mocks.loadOriginalAudio
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
  mocks.resolveNoteCandidate.mockResolvedValue(mocks.store.current)
  mocks.loadParts.mockResolvedValue(mocks.store.parts)
  mocks.loadAudioPart.mockResolvedValue('blob:audio')
  mocks.loadOriginalAudio.mockResolvedValue('/api/course-audio/ticket')
})

describe('CourseDetailView', () => {
  it('uses a readable section label without a duplicate status tag', async () => {
    mocks.store.current = { ...mocks.store.current, sourceType: 'IMPORT' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.get('[data-test="course-eyebrow"]').text()).toBe('课程记录')
    expect(wrapper.find('.el-tag').exists()).toBe(false)
    expect(wrapper.get('[data-test="load-original-audio"]').classes()).toContain('secondary-action')
    expect(wrapper.get('[data-test="load-original-audio"]').classes()).not.toContain('is-text')
  })

  it('explains how to resume an unfinished recording upload', async () => {
    mocks.store.current = { ...mocks.store.current, status: 'UPLOADING', sourceType: 'IMPORT' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()
    expect(wrapper.text()).toContain('回到课程列表，选择同一份录音文件继续上传')
  })

  it('keeps completed transcript sections visible after a later section fails', async () => {
    mocks.store.current = { ...mocks.store.current, status: 'FAILED', transcript: '已完成第一段',
      errorMessage: '第二段失败' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()
    expect(wrapper.text()).toContain('已完成第一段')
  })
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

    await wrapper.get('[data-test="load-course-audio"]').trigger('click')
    await flushPromises()

    expect(mocks.loadAudioPart).toHaveBeenCalledWith(9, 1)
    expect(wrapper.get('audio').attributes('src')).toBe('blob:audio')
  })

  it('plays recorded parts through one player and advances automatically', async () => {
    mocks.store.parts = [
      { id: 1, partNumber: 1, durationSeconds: 60, fileSize: 5 },
      { id: 2, partNumber: 2, durationSeconds: 60, fileSize: 5 },
    ]
    mocks.loadParts.mockResolvedValue(mocks.store.parts)
    mocks.loadAudioPart.mockImplementation(async (_courseId: number, part: number) => `blob:audio-${part}`)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="load-course-audio"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('audio')).toHaveLength(1)
    expect(wrapper.get('audio').attributes('src')).toBe('blob:audio-1')

    await wrapper.get('audio').trigger('ended')
    await flushPromises()
    expect(mocks.loadAudioPart).toHaveBeenCalledWith(9, 2)
    expect(wrapper.get('audio').attributes('src')).toBe('blob:audio-2')
  })

  it('offers explicit choices for a regenerated note candidate', async () => {
    mocks.store.current = { ...mocks.store.current, noteCandidate: '新生成笔记' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    expect(wrapper.text()).toContain('新生成笔记')
    await wrapper.get('[data-test="append-note-candidate"]').trigger('click')
    expect(mocks.resolveNoteCandidate).toHaveBeenCalledWith(9, 'APPEND')
  })

  it('streams the imported original M4A through a playback URL', async () => {
    mocks.store.current = { ...mocks.store.current, sourceType: 'IMPORT' }
    mocks.loadOne.mockResolvedValue(mocks.store.current)
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="load-original-audio"]').trigger('click')
    await flushPromises()

    expect(mocks.loadOriginalAudio).toHaveBeenCalledWith(9)
    expect(wrapper.get('[data-test="original-audio"]').attributes('src')).toBe('/api/course-audio/ticket')
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

  it('shows a stable loading state before the course detail arrives', () => {
    mocks.store.current = null
    mocks.loadOne.mockReturnValue(new Promise(() => undefined))

    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })

    expect(wrapper.get('[data-test="course-detail-loading"]').text()).toContain('正在加载课程内容')
  })

  it('prevents duplicate note saves while the request is running', async () => {
    let finishSave!: (value: unknown) => void
    mocks.saveNote.mockReturnValue(new Promise(resolve => { finishSave = resolve }))
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()
    await wrapper.get('[data-test="course-note"]').setValue('# 新笔记')

    await wrapper.get('[data-test="save-course-note"]').trigger('click')

    expect(wrapper.get('[data-test="save-course-note"]').classes()).toContain('is-loading')
    finishSave(mocks.store.current)
    await flushPromises()
  })

  it('shows loading on the selected audio button while fetching audio', async () => {
    let finishLoad!: (value: string) => void
    mocks.loadAudioPart.mockReturnValue(new Promise(resolve => { finishLoad = resolve }))
    const wrapper = mount(CourseDetailView, { global: { stubs: { RouterLink: RouterLinkStub } } })
    await flushPromises()

    await wrapper.get('[data-test="load-course-audio"]').trigger('click')

    expect(wrapper.get('[data-test="load-course-audio"]').classes()).toContain('is-loading')
    finishLoad('blob:audio')
    await flushPromises()
  })
})
