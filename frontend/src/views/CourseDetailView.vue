<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Download, Refresh } from '@element-plus/icons-vue'
import { ElButton, ElIcon, ElInput, ElMessageBox, ElProgress } from 'element-plus'
import { useRoute } from 'vue-router'
import { useCourseStore } from '../features/course/courseStore'

const route = useRoute()
const store = useCourseStore()
store.reset()
const courseId = Number(route.params.id)
const course = computed(() => store.current?.id === courseId ? store.current : null)
const note = ref('')
const notice = ref('')
const downloadPending = ref(false)
const notePending = ref(false)
const initialLoading = ref(true)
const noteSaving = ref(false)
const retryPending = ref(false)
const originalAudioPending = ref(false)
const audioLoading = ref(false)
const currentPartIndex = ref(0)
const audioSource = ref('')
const originalAudioSource = ref('')
let pollTimer: number | null = null

watch(() => course.value?.noteContent, (content) => {
  note.value = content ?? ''
}, { immediate: true })

onMounted(async () => {
  try {
    await Promise.all([refresh(), store.loadParts(courseId).catch(() => [])])
    if (course.value?.status === 'PROCESSING') startPolling()
  } finally {
    initialLoading.value = false
  }
})

onBeforeUnmount(() => {
  if (pollTimer !== null) window.clearInterval(pollTimer)
})

async function refresh() {
  try {
    const course = await store.loadOne(courseId)
    if (course?.status !== 'PROCESSING' && pollTimer !== null) {
      window.clearInterval(pollTimer)
      pollTimer = null
    }
  } catch {
    // Store 已提供统一错误信息。
  }
}

function startPolling() {
  if (pollTimer !== null) return
  // 转写是分钟级操作，用低频轮询保持实现轻量。
  pollTimer = window.setInterval(() => void refresh(), 5000)
}

async function saveNote() {
  if (!note.value.trim() || noteSaving.value) return
  noteSaving.value = true
  try {
    await store.saveNote(courseId, note.value)
    notice.value = '笔记已保存'
  } catch {
    // Store 已提供错误信息。
  } finally {
    noteSaving.value = false
  }
}

async function retry() {
  if (retryPending.value) return
  retryPending.value = true
  await store.retry(courseId).then(() => {
    notice.value = ''
    startPolling()
  }).catch(() => undefined).finally(() => { retryPending.value = false })
}

async function generateNote() {
  if (notePending.value) return
  notePending.value = true
  await store.generateNote(courseId).then(() => {
    notice.value = '正在生成笔记，可以稍后回来查看'
    startPolling()
  }).catch(() => undefined).finally(() => { notePending.value = false })
}

async function loadAudioAt(index: number, autoplay = false) {
  const part = store.parts[index]
  if (!part || audioLoading.value) return
  audioLoading.value = true
  try {
    const source = await store.loadAudioPart(courseId, part.partNumber)
    if (source) {
      currentPartIndex.value = index
      audioSource.value = source
      if (autoplay) requestAnimationFrame(() => {
        void document.querySelector<HTMLAudioElement>('[data-test="continuous-audio"]')?.play()
      })
    }
  } catch {
    // Store 已提供错误信息。
  } finally {
    audioLoading.value = false
  }
}

async function playNextPart() {
  await loadAudioAt(currentPartIndex.value + 1, true)
}

async function resolveCandidate(action: 'REPLACE' | 'APPEND' | 'DISCARD') {
  if (action === 'REPLACE') {
    await ElMessageBox.confirm('确定用新结果覆盖当前笔记吗？', '替换笔记', { type: 'warning' })
      .catch(() => { throw new Error('cancelled') })
  }
  await store.resolveNoteCandidate(courseId, action).then(() => {
    notice.value = action === 'DISCARD' ? '已保留原笔记' : '笔记已更新'
  }).catch(() => undefined)
}

async function loadOriginalAudio() {
  if (originalAudioPending.value) return
  originalAudioPending.value = true
  try {
    originalAudioSource.value = await store.loadOriginalAudio(courseId)
  } catch {
    originalAudioSource.value = ''
  } finally {
    originalAudioPending.value = false
  }
}

async function downloadNote() {
  if (!course.value || downloadPending.value) return
  downloadPending.value = true
  await store.downloadNote(courseId, course.value.title)
    .then(() => { notice.value = '笔记已开始下载' })
    .catch(() => undefined)
    .finally(() => { downloadPending.value = false })
}
</script>

<template>
  <div class="detail-page">
    <main class="detail-shell">
      <RouterLink class="back-link" :to="{ name: 'courses' }"><ElIcon><ArrowLeft /></ElIcon> 返回课程记录</RouterLink>
      <p class="eyebrow" data-test="course-eyebrow">课程记录</p>
      <div v-if="course" class="detail-heading">
        <div><h1>{{ course.title }}</h1><span>{{ Math.ceil(course.durationSeconds / 60) }} 分钟</span></div>
      </div>

      <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
      <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>

      <section v-if="initialLoading" class="processing-card" data-test="course-detail-loading" role="status">
        <span class="spinner"></span><h2>正在加载课程内容…</h2>
      </section>

      <section v-else-if="course?.status === 'UPLOADING'" class="processing-card">
        <h2>录音还没上传完</h2>
        <p>回到课程列表，选择同一份录音文件继续上传。</p>
      </section>

      <section v-else-if="course?.status === 'PROCESSING' && !course.noteContent" class="processing-card" aria-busy="true">
        <span class="spinner"></span><h2>{{ course.transcript ? '正在生成笔记' : '正在提取录音文字' }}</h2>
        <ElProgress
          data-test="course-progress"
          :percentage="course.processingProgress"
          :stroke-width="14"
          striped
          striped-flow
        />
        <p>可以离开此页面，稍后回来查看。</p>
      </section>

      <section v-else-if="course?.status === 'FAILED'" class="failed-card">
        <h2>录音处理失败</h2>
        <p>{{ course.errorMessage }}</p>
        <pre v-if="course.transcript">{{ course.transcript }}</pre>
        <ElButton class="standard-action-button" data-test="retry-course" round type="primary" :loading="retryPending" @click="retry"><ElIcon><Refresh /></ElIcon>重新处理</ElButton>
      </section>

      <div v-else-if="course" class="content-grid">
        <section class="content-card transcript-card">
          <span>原始内容</span><h2>完整转写</h2>
          <pre>{{ course.transcript || '暂无转写内容' }}</pre>
          <div class="audio-list">
            <h3>原始录音</h3>
            <div v-if="course.sourceType === 'IMPORT'" class="audio-part original-audio">
              <span>上传的录音</span>
              <audio v-if="originalAudioSource" data-test="original-audio" controls :src="originalAudioSource"></audio>
              <ElButton v-else class="secondary-action" data-test="load-original-audio" round :loading="originalAudioPending" @click="loadOriginalAudio">加载原始录音</ElButton>
            </div>
            <div v-else-if="store.parts.length" class="audio-part continuous-audio">
              <span>连续播放 · 第 {{ currentPartIndex + 1 }}/{{ store.parts.length }} 段</span>
              <audio v-if="audioSource" data-test="continuous-audio" controls :src="audioSource" @ended="playNextPart"></audio>
              <ElButton v-else class="secondary-action" data-test="load-course-audio" round :loading="audioLoading" @click="loadAudioAt(0)">播放完整录音</ElButton>
            </div>
          </div>
        </section>
        <section class="content-card note-card">
          <span>整理结果</span><h2>课程笔记</h2>
          <div v-if="course.status === 'PROCESSING' && course.noteContent" class="regeneration-progress" role="status">
            正在重新生成，当前笔记已安全保留。
            <ElProgress data-test="course-progress" :percentage="course.processingProgress" :stroke-width="8" />
          </div>
          <p v-if="course.status === 'READY' && course.errorMessage" data-test="note-generation-error" class="note-error" role="alert">{{ course.errorMessage }}</p>
          <div v-if="course.status === 'TRANSCRIBED'" class="generate-box">
            <p v-if="course.errorMessage" data-test="note-generation-error" class="note-error" role="alert">{{ course.errorMessage }}</p>
            <p>文字已经保存。需要时再调用大模型整理成笔记。</p>
            <ElButton data-test="generate-course-note" round type="primary" :loading="notePending" @click="generateNote">生成笔记</ElButton>
          </div>
          <template v-else>
            <ElInput v-model="note" data-test="course-note" type="textarea" :rows="18" />
          <div v-if="course.noteCandidate" class="candidate-box">
            <strong>新生成的笔记</strong>
            <pre>{{ course.noteCandidate }}</pre>
            <div class="note-actions">
              <ElButton data-test="replace-note-candidate" round type="primary" @click="resolveCandidate('REPLACE')">替换原笔记</ElButton>
              <ElButton data-test="append-note-candidate" round @click="resolveCandidate('APPEND')">追加为补充</ElButton>
              <ElButton data-test="discard-note-candidate" round @click="resolveCandidate('DISCARD')">保留原笔记</ElButton>
            </div>
          </div>
          <div class="note-actions">
            <ElButton data-test="save-course-note" round type="primary" :loading="noteSaving" :disabled="!note.trim()" @click="saveNote">保存笔记</ElButton>
            <ElButton v-if="course.status === 'READY' && !course.noteCandidate" data-test="regenerate-course-note" round :loading="notePending" @click="generateNote">重新生成</ElButton>
            <ElButton
              data-test="download-course-note"
              round
              :loading="downloadPending"
              @click="downloadNote"
            ><ElIcon><Download /></ElIcon>下载 DOCX</ElButton>
          </div>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.detail-shell { width: min(1320px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.back-link { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 28px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.eyebrow { margin: 0 0 10px; color: #f05a18; font-size: 16px; font-weight: 850; letter-spacing: .04em; }
.content-card > span { margin: 0 0 8px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.detail-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 22px; }
.detail-heading h1 { margin: 0; font-size: clamp(34px, 5vw, 64px); letter-spacing: -.055em; }
.detail-heading span { display: block; margin-top: 10px; color: #77736c; }
.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 38px; }
.content-card, .processing-card, .failed-card { padding: clamp(24px, 3vw, 34px); border-radius: 28px; background: #fff; }
.content-card h2 { margin: 0 0 20px; font-size: 26px; }
.content-card pre { max-height: 560px; overflow: auto; margin: 0; color: #57534d; font: inherit; font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.audio-list { margin-top: 24px; padding-top: 20px; border-top: 1px solid #eeeae3; }
.audio-list h3 { margin: 0 0 12px; font-size: 15px; }
.audio-part { display: grid; min-height: 48px; align-items: center; grid-template-columns: minmax(110px, 1fr) minmax(120px, 2fr); gap: 12px; color: #77736c; font-size: 12px; }
.audio-part audio { width: 100%; height: 38px; }
.audio-part :deep(.el-button) { justify-self: start; }
.note-card { background: #24262d; color: #fff; }
.generate-box { padding: 28px 0; color: #c3c4c8; line-height: 1.7; }
.generate-box .note-error { padding: 11px 13px; border-radius: 12px; color: #ffd2c1; background: #4b2a25; }
.regeneration-progress, .candidate-box { margin-bottom: 16px; padding: 16px; border-radius: 16px; background: #343740; color: #e8e8ea; }
.candidate-box pre { max-height: 260px; margin: 12px 0; color: #e8e8ea; }
.note-card :deep(.el-textarea__inner) { padding: 18px; border: 0; border-radius: 18px; box-shadow: none; line-height: 1.7; }
.note-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.note-card :deep(.el-button) { min-height: 44px; margin: 0; }
.note-card :deep(.el-button--primary), .failed-card :deep(.el-button--primary) { --el-button-bg-color: #17181c; --el-button-border-color: #17181c; --el-button-hover-bg-color: #f05a18; --el-button-hover-border-color: #f05a18; }
.processing-card, .failed-card { margin-top: 38px; text-align: center; }
.failed-card pre { max-height: 240px; overflow: auto; padding: 16px; border-radius: 14px; background: #f6f3ed; text-align: left; white-space: pre-wrap; }
.processing-card h2, .failed-card h2 { margin: 14px 0 8px; }
.processing-card p, .failed-card p { color: #77736c; }
.processing-card :deep(.el-progress) { width: min(520px, 100%); margin: 20px auto 0; }
.spinner { display: inline-block; width: 42px; height: 42px; border: 6px solid #eee8dc; border-top-color: #f05a18; border-radius: 50%; animation: spin 1s linear infinite; }
.error-notice, .success-notice { margin: 20px 0 0; padding: 12px 15px; border-radius: 15px; font-size: 13px; }
.error-notice { color: #9f2d13; background: #fff0e9; }.success-notice { color: #554100; background: #fff1a8; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spinner { animation: none; } }
@media (max-width: 860px) { .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .detail-page { padding: 0; } .detail-shell { min-height: 100vh; border-radius: 0; } }
</style>
