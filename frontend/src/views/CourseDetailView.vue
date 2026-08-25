<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Download, Refresh } from '@element-plus/icons-vue'
import { ElButton, ElIcon, ElInput, ElProgress, ElTag } from 'element-plus'
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
let pollTimer: number | null = null

watch(() => course.value?.noteContent, (content) => {
  note.value = content ?? ''
}, { immediate: true })

onMounted(async () => {
  await refresh()
  if (course.value?.status === 'PROCESSING') startPolling()
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
  if (!note.value.trim()) return
  await store.saveNote(courseId, note.value).then(() => {
    notice.value = '笔记已保存'
  }).catch(() => undefined)
}

async function retry() {
  await store.retry(courseId).then(() => {
    notice.value = ''
    startPolling()
  }).catch(() => undefined)
}

async function downloadNote() {
  if (!course.value || downloadPending.value) return
  downloadPending.value = true
  await store.downloadNote(courseId, course.value.title)
    .catch(() => undefined)
    .finally(() => { downloadPending.value = false })
}
</script>

<template>
  <div class="detail-page">
    <main class="detail-shell">
      <RouterLink class="back-link" :to="{ name: 'courses' }"><ElIcon><ArrowLeft /></ElIcon> 返回课程记录</RouterLink>
      <p class="eyebrow">COURSE DETAIL</p>
      <div v-if="course" class="detail-heading">
        <div><h1>{{ course.title }}</h1><span>{{ Math.ceil(course.durationSeconds / 60) }} 分钟</span></div>
        <ElTag round :type="course.status === 'FAILED' ? 'danger' : course.status === 'READY' ? 'success' : 'warning'">
          {{ course.status === 'READY' ? '已完成' : course.status === 'FAILED' ? '处理失败' : '整理中' }}
        </ElTag>
      </div>

      <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
      <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>

      <section v-if="course?.status === 'PROCESSING'" class="processing-card" aria-busy="true">
        <span class="spinner"></span><h2>正在转写并生成笔记</h2>
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
        <h2>这次整理没有完成</h2>
        <p>{{ course.errorMessage }}</p>
        <ElButton data-test="retry-course" round type="primary" @click="retry"><ElIcon><Refresh /></ElIcon>重新处理</ElButton>
      </section>

      <div v-else-if="course" class="content-grid">
        <section class="content-card">
          <span>TRANSCRIPT</span><h2>完整转写</h2>
          <pre>{{ course.transcript || '暂无转写内容' }}</pre>
        </section>
        <section class="content-card note-card">
          <span>MY NOTES</span><h2>课程笔记</h2>
          <ElInput v-model="note" data-test="course-note" type="textarea" :rows="18" />
          <div class="note-actions">
            <ElButton data-test="save-course-note" round type="primary" @click="saveNote">保存笔记</ElButton>
            <ElButton
              data-test="download-course-note"
              round
              :loading="downloadPending"
              @click="downloadNote"
            ><ElIcon><Download /></ElIcon>下载 DOCX</ElButton>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.detail-shell { width: min(1320px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.back-link { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 28px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.eyebrow, .content-card > span { margin: 0 0 8px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.detail-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 22px; }
.detail-heading h1 { margin: 0; font-size: clamp(34px, 5vw, 64px); letter-spacing: -.055em; }
.detail-heading span { display: block; margin-top: 10px; color: #77736c; }
.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 38px; }
.content-card, .processing-card, .failed-card { padding: clamp(24px, 3vw, 34px); border-radius: 28px; background: #fff; }
.content-card h2 { margin: 0 0 20px; font-size: 26px; }
.content-card pre { max-height: 560px; overflow: auto; margin: 0; color: #57534d; font: inherit; font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.note-card { background: #24262d; color: #fff; }
.note-card :deep(.el-textarea__inner) { padding: 18px; border: 0; border-radius: 18px; box-shadow: none; line-height: 1.7; }
.note-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.note-card :deep(.el-button) { min-height: 44px; margin: 0; }
.note-card :deep(.el-button--primary), .failed-card :deep(.el-button--primary) { --el-button-bg-color: #17181c; --el-button-border-color: #17181c; --el-button-hover-bg-color: #f05a18; --el-button-hover-border-color: #f05a18; }
.processing-card, .failed-card { margin-top: 38px; text-align: center; }
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
