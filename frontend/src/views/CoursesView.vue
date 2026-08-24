<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, Headset, Microphone, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { ElButton, ElIcon, ElInput, ElTag } from 'element-plus'
import { useCourseStore } from '../features/course/courseStore'
import { useCourseRecorder } from '../features/course/useCourseRecorder'
import { useRouter } from 'vue-router'
import type { CourseStatus } from '../features/course/types'

const store = useCourseStore()
store.reset()
const recorder = useCourseRecorder(store)
const router = useRouter()
const title = ref('')

const timeText = computed(() => {
  const minutes = Math.floor(recorder.elapsedSeconds.value / 60).toString().padStart(2, '0')
  const seconds = (recorder.elapsedSeconds.value % 60).toString().padStart(2, '0')
  return `${minutes}:${seconds}`
})

const statusText: Record<CourseStatus, string> = {
  RECORDING: '录音中',
  PROCESSING: '整理中',
  READY: '已完成',
  FAILED: '处理失败',
}

onMounted(() => {
  void store.loadAll().catch(() => undefined)
})

async function startRecording() {
  if (!title.value.trim()) return
  await recorder.start(title.value).catch(() => undefined)
}

async function stopRecording() {
  await recorder.stop().catch(() => undefined)
  await store.loadAll().catch(() => undefined)
  if (recorder.status.value === 'processing' && store.current) {
    await router.push({ name: 'course-detail', params: { id: store.current.id } })
  }
}

async function retryUpload() {
  await recorder.retryUploads().catch(() => undefined)
  if (recorder.status.value === 'processing' && store.current) {
    await router.push({ name: 'course-detail', params: { id: store.current.id } })
  }
}

function formatDuration(seconds: number) {
  if (!seconds) return '尚未完成录音'
  return `${Math.floor(seconds / 60)} 分钟`
}
</script>

<template>
  <div class="course-page">
    <div class="course-shell">
      <header class="course-header">
        <div>
          <RouterLink class="back-link" :to="{ name: 'home' }"><ElIcon><ArrowLeft /></ElIcon> 返回工作台</RouterLink>
          <p>COURSE NOTE AGENT</p>
          <h1>录下课堂，整理成你的笔记。</h1>
          <span>最长 90 分钟。录音结束后统一转写，不打断上课节奏。</span>
        </div>
        <div class="header-mark"><ElIcon><Headset /></ElIcon></div>
      </header>

      <main class="course-layout">
        <section class="recorder-card" aria-live="polite">
          <div class="card-heading"><span>NEW RECORDING</span><h2>开始一节课程</h2></div>
          <label class="field-label" for="course-title">课程名称</label>
          <ElInput
            id="course-title"
            v-model="title"
            data-test="course-title"
            maxlength="160"
            placeholder="例如：Java 并发编程"
            :disabled="recorder.isActive.value"
          />

          <div class="record-display" :class="{ active: recorder.status.value === 'recording' }">
            <span class="record-dot" aria-hidden="true"></span>
            <strong>{{ timeText }}</strong>
            <small>
              {{ recorder.status.value === 'idle' ? '等待开始' : recorder.status.value === 'processing' ? '正在生成笔记' : `已上传 ${recorder.uploadedParts.value} 个分片` }}
            </small>
          </div>

          <p v-if="recorder.error.value || store.error" class="error-notice" role="alert">
            {{ recorder.error.value || store.error }}
          </p>

          <div class="record-actions">
            <ElButton v-if="recorder.canRetry.value" data-test="retry-upload" type="primary" round @click="retryUpload">重试上传</ElButton>
            <ElButton
              v-else-if="!recorder.isActive.value"
              data-test="start-recording"
              type="primary"
              round
              :disabled="!title.trim() || recorder.status.value === 'processing'"
              @click="startRecording"
            ><ElIcon><Microphone /></ElIcon>开始录音</ElButton>
            <template v-else>
              <ElButton v-if="recorder.status.value === 'recording'" round @click="recorder.pause">
                <ElIcon><VideoPause /></ElIcon>暂停
              </ElButton>
              <ElButton v-else round @click="recorder.resume">
                <ElIcon><VideoPlay /></ElIcon>继续
              </ElButton>
              <ElButton type="danger" round @click="stopRecording">结束并整理</ElButton>
            </template>
          </div>
        </section>

        <aside class="history-card">
          <div class="history-heading"><span>ARCHIVE</span><h2>课程记录</h2></div>
          <div v-if="store.courses.length" class="course-list">
            <article v-for="course in store.courses" :key="course.id">
              <div>
                <ElTag round :type="course.status === 'FAILED' ? 'danger' : course.status === 'READY' ? 'success' : 'warning'">
                  {{ statusText[course.status] }}
                </ElTag>
                <time>{{ new Date(course.createdAt).toLocaleDateString('zh-CN') }}</time>
              </div>
              <h3>{{ course.title }}</h3>
              <p>{{ formatDuration(course.durationSeconds) }}</p>
              <RouterLink :to="{ name: 'course-detail', params: { id: course.id } }">查看笔记 →</RouterLink>
            </article>
          </div>
          <div v-else class="empty-state">还没有课程记录，从左侧开始第一节课。</div>
        </aside>
      </main>
    </div>
  </div>
</template>

<style scoped>
.course-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.course-shell { width: min(1440px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.course-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; }
.back-link { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 28px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.course-header p, .card-heading span, .history-heading span { margin: 0 0 9px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.course-header h1 { max-width: 760px; margin: 0; font-size: clamp(34px, 4.6vw, 66px); line-height: 1.05; letter-spacing: -.06em; }
.course-header > div > span { display: block; margin-top: 15px; color: #78736b; }
.header-mark { display: grid; width: 92px; height: 92px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #fff; background: #f05a18; font-size: 34px; }
.course-layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(340px, .75fr); gap: 22px; margin-top: 42px; }
.recorder-card, .history-card { padding: clamp(24px, 3vw, 36px); border-radius: 30px; background: #fff; }
.card-heading h2, .history-heading h2 { margin: 0 0 26px; font-size: 29px; letter-spacing: -.04em; }
.field-label { display: block; margin-bottom: 9px; color: #65615b; font-size: 13px; font-weight: 750; }
.recorder-card :deep(.el-input__wrapper) { min-height: 50px; border-radius: 18px; box-shadow: 0 0 0 1px #e2ded6 inset; }
.record-display { display: grid; min-height: 210px; margin-top: 22px; place-items: center; align-content: center; border-radius: 26px; background: #24262d; color: #fff; }
.record-display strong { margin: 10px 0 4px; font-size: clamp(44px, 7vw, 76px); letter-spacing: -.05em; }
.record-display small { color: #b9bac0; }
.record-dot { width: 18px; height: 18px; border: 5px solid #4a4c54; border-radius: 50%; background: #777982; }
.record-display.active .record-dot { border-color: #ffb394; background: #f05a18; box-shadow: 0 0 0 8px rgb(240 90 24 / 15%); }
.record-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 20px; }
.record-actions :deep(.el-button) { min-width: 132px; min-height: 46px; margin: 0; }
.record-actions :deep(.el-button--primary) { --el-button-bg-color: #17181c; --el-button-border-color: #17181c; --el-button-hover-bg-color: #f05a18; --el-button-hover-border-color: #f05a18; }
.error-notice { margin: 16px 0 0; padding: 12px 15px; border-radius: 15px; color: #9f2d13; background: #fff0e9; font-size: 13px; }
.history-card { align-self: start; color: #fff; background: #24262d; }
.course-list { display: grid; gap: 12px; }
.course-list article { padding: 18px; border-radius: 20px; background: #30323a; }
.course-list article > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.course-list time { color: #aaaab0; font-size: 11px; }
.course-list h3 { margin: 14px 0 7px; font-size: 18px; }
.course-list p { margin: 0 0 12px; color: #aaaab0; font-size: 12px; }
.course-list a { color: #f89a6f; font-size: 12px; font-weight: 750; text-decoration: none; }
.empty-state { padding: 34px 18px; border: 1px dashed #4b4d55; border-radius: 20px; color: #aaaab0; font-size: 13px; text-align: center; }
@media (max-width: 940px) { .course-layout { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .course-page { padding: 0; } .course-shell { min-height: 100vh; border-radius: 0; } .header-mark { display: none; } .record-actions { display: grid; } .record-actions :deep(.el-button) { width: 100%; } }
</style>
