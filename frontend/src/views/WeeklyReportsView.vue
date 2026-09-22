<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { ElButton, ElIcon, ElMessage, ElProgress } from 'element-plus'
import WeeklyReportForm from '../components/WeeklyReportForm.vue'
import { useWeeklyReportStore } from '../features/weeklyReport/weeklyReportStore'
import type { WeeklyReportInput } from '../features/weeklyReport/types'
import type { DocxImportResult } from '../features/weeklyReport/types'

const store = useWeeklyReportStore()
const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const notice = ref('')
const batchDrafts = ref<BatchDraft[]>([])
const batchPending = ref(false)
const batchSaving = ref(false)
const batchNotice = ref('')
const importNotice = ref('')
const singleImportReady = ref(false)
const recognitionFailures = ref(0)
const importPhase = ref<'recognizing' | 'saving' | null>(null)
const progressDone = ref(0)
const progressTotal = ref(0)
const batchPage = ref(1)
const pageSize = 5
const pageCount = computed(() => Math.ceil(batchDrafts.value.length / pageSize))
const visibleDrafts = computed(() => batchDrafts.value.slice((batchPage.value - 1) * pageSize, batchPage.value * pageSize))
const progressPercent = computed(() => progressTotal.value ? Math.round(progressDone.value / progressTotal.value * 100) : 0)
const historyPending = ref(false)
const formPending = ref(false)

interface BatchDraft extends WeeklyReportInput {
  status: 'pending' | 'saved' | 'failed'
  message: string
}

onMounted(() => {
  store.imported = null
  void loadHistory()
})

watch([year, month], ([nextYear, nextMonth]) => {
  void loadHistory(nextYear, nextMonth)
})

async function loadHistory(nextYear = year.value, nextMonth = month.value) {
  historyPending.value = true
  try {
    await store.loadMonth(nextYear, nextMonth)
  } catch {
    // Store 已统一提供面向用户的错误信息，页面只需阻止未处理的 Promise。
  } finally {
    historyPending.value = false
  }
}

async function save(input: WeeklyReportInput) {
  if (formPending.value) return
  formPending.value = true
  if (singleImportReady.value) {
    importPhase.value = 'saving'
    progressDone.value = 0
    progressTotal.value = 1
  }
  try {
    const created = await store.create(input)
    if (!created) return
    notice.value = '周报已保存'
    if (singleImportReady.value) {
      ElMessage.success('周报导入成功，已保存到历史周报')
      singleImportReady.value = false
      importNotice.value = ''
    }
    const savedDate = new Date(`${created.weekStartDate}T00:00:00`)
    const savedYear = savedDate.getFullYear()
    const savedMonth = savedDate.getMonth() + 1
    if (year.value === savedYear && month.value === savedMonth) {
      await loadHistory()
    } else {
      // 切换筛选条件后由统一的 watch 发起一次查询，避免保存后重复请求。
      year.value = savedYear
      month.value = savedMonth
    }
  } catch {
    notice.value = ''
  } finally {
    formPending.value = false
    if (importPhase.value === 'saving') {
      progressDone.value = 1
      importPhase.value = null
    }
  }
}

async function importFiles(files: File[]) {
  notice.value = ''
  importNotice.value = ''
  batchNotice.value = ''
  batchDrafts.value = []
  batchPage.value = 1
  singleImportReady.value = false
  recognitionFailures.value = 0
  importPhase.value = 'recognizing'
  progressDone.value = 0
  progressTotal.value = files.length
  if (files.length === 1 && files[0]) {
    formPending.value = true
    try {
      const result = await store.importDocx(files[0])
      if (result) {
        singleImportReady.value = true
        importNotice.value = `${files[0].name} 已识别，请检查内容后保存。`
      }
    } catch {
      // Store 已提供错误提示。
    } finally {
      formPending.value = false
      progressDone.value = 1
      importPhase.value = null
    }
    return
  }

  batchPending.value = true
  const recognized: DocxImportResult[] = []
  const failedNames: string[] = []
  for (let index = 0; index < files.length; index += 1) {
    const file = files[index]!
    try {
      const result = await store.recognizeDocx(file, { index: index + 1, total: files.length })
      if (result) recognized.push(result)
    } catch {
      failedNames.push(file.name)
    } finally {
      progressDone.value = index + 1
    }
  }
  batchDrafts.value = mergeByWeek(recognized)
  recognitionFailures.value = failedNames.length
  batchPending.value = false
  importPhase.value = null
  if (failedNames.length) batchNotice.value = `${failedNames.length} 个文件识别失败，可以重新选择上传。`
}

function mergeText(first: string, second: string | null) {
  if (!second?.trim()) return first
  return first.trim() ? `${first.trimEnd()}\n${second.trim()}` : second.trim()
}

function mergeByWeek(results: DocxImportResult[]): BatchDraft[] {
  const grouped = new Map<string, BatchDraft>()
  results.forEach((result, index) => {
    const key = result.weekStartDate ?? `missing-${index}`
    const current = grouped.get(key)
    if (current) {
      current.coreWork = mergeText(current.coreWork, result.coreWork)
      current.problems = mergeText(current.problems, result.problems)
      current.nextWeekPlan = mergeText(current.nextWeekPlan, result.nextWeekPlan)
      current.sourceFileName = `${current.sourceFileName}、${result.sourceFileName}`.slice(0, 255)
      return
    }
    grouped.set(key, {
      weekStartDate: result.weekStartDate ?? '',
      coreWork: result.coreWork,
      problems: result.problems ?? '',
      nextWeekPlan: result.nextWeekPlan ?? '',
      sourceFileName: result.sourceFileName,
      status: 'pending',
      message: result.weekStartDate ? '' : '未识别到日期，请选择所属周',
    })
  })
  return [...grouped.values()]
}

function normalizeToMonday(value: string) {
  if (!value) return ''
  const date = new Date(`${value}T00:00:00`)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  return date.toLocaleDateString('en-CA')
}

async function saveBatch() {
  if (batchSaving.value) return
  if (batchDrafts.value.some((draft) => !draft.weekStartDate)) {
    batchNotice.value = '请先为未识别日期的文件选择所属周。'
    return
  }
  batchSaving.value = true
  const pending = batchDrafts.value.filter((item) => item.status !== 'saved')
  importPhase.value = 'saving'
  progressDone.value = 0
  progressTotal.value = pending.length
  let saved = 0
  let failed = 0
  for (const draft of pending) {
    draft.weekStartDate = normalizeToMonday(draft.weekStartDate)
    try {
      const result = await store.create(draft)
      if (result) {
        draft.status = 'saved'
        draft.message = '已保存'
        saved += 1
      } else {
        draft.status = 'failed'
        draft.message = '保存未完成，请重试'
        failed += 1
      }
    } catch {
      draft.status = 'failed'
      draft.message = '该周可能已有周报，请检查后重试'
      failed += 1
    } finally {
      progressDone.value += 1
    }
  }
  batchSaving.value = false
  importPhase.value = null
  batchNotice.value = `成功导入 ${saved} 份；识别失败 ${recognitionFailures.value} 个文件，保存失败 ${failed} 份。`
  if (failed || recognitionFailures.value) ElMessage.warning(batchNotice.value)
  else ElMessage.success(batchNotice.value)
  try {
    await loadHistory()
  } catch {
    // 保存结果已经显示，历史列表加载错误使用 Store 提示。
  }
}
</script>

<template>
  <div class="report-page">
    <div class="report-shell">
      <header class="report-header">
        <div>
          <RouterLink class="back-link" :to="{ name: 'home' }"><ElIcon><ArrowLeft /></ElIcon> 返回工作台</RouterLink>
          <p>周报</p>
          <h1>记录本周工作</h1>
          <span>在线填写，也可以导入 DOCX。</span>
        </div>
        <div class="header-mark">W / 01</div>
      </header>

      <main class="report-layout">
        <section class="composer-section">
          <div class="section-title"><span>填写内容</span><h2>新建周报</h2></div>
          <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>
          <p v-if="importNotice" class="success-notice" data-test="single-import-notice" role="status">{{ importNotice }}</p>
          <p v-if="batchNotice && !batchDrafts.length" class="error-notice" role="alert">{{ batchNotice }}</p>
          <div v-if="importPhase" class="import-progress" role="status" aria-live="polite">
            <span>{{ importPhase === 'recognizing' ? '正在识别' : '正在保存' }}周报 {{ progressDone }}/{{ progressTotal }} 份</span>
            <ElProgress :percentage="progressPercent" :stroke-width="10" :show-text="false" />
          </div>
          <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
          <WeeklyReportForm
            allow-multiple
            :imported="store.imported"
            :loading="formPending"
            @save="save"
            @import="importFiles"
          />
          <section v-if="batchPending || batchDrafts.length" class="batch-panel">
            <div class="batch-heading">
              <div><span>批量预览</span><h2>{{ batchPending ? '正在识别文件' : `${batchDrafts.length} 份待确认周报` }}</h2></div>
              <ElButton class="standard-action-button" data-test="save-batch-reports" type="primary" round
                :loading="batchSaving" :disabled="batchPending || batchDrafts.every(item => item.status === 'saved')"
                @click="saveBatch">确认导入</ElButton>
            </div>
            <p v-if="batchNotice" class="batch-notice" role="status">{{ batchNotice }}</p>
            <article v-for="draft in visibleDrafts" :key="draft.sourceFileName ?? draft.weekStartDate" data-test="batch-report-item" class="batch-item">
              <div class="batch-row"><strong>{{ draft.sourceFileName }}</strong><span :class="draft.status">{{ draft.message || '待保存' }}</span></div>
              <label>所属周<input v-model="draft.weekStartDate" class="period-control" type="date" @change="draft.weekStartDate = normalizeToMonday(draft.weekStartDate)" /></label>
              <label>核心工作<textarea v-model="draft.coreWork" rows="4"></textarea></label>
              <div class="batch-fields"><label>遇到的问题<textarea v-model="draft.problems" rows="3"></textarea></label><label>下周计划<textarea v-model="draft.nextWeekPlan" rows="3"></textarea></label></div>
            </article>
            <nav v-if="pageCount > 1" class="batch-pagination" aria-label="导入预览分页">
              <ElButton data-test="batch-prev" round :disabled="batchPage === 1" @click="batchPage--">上一页</ElButton>
              <span>第 {{ batchPage }} / {{ pageCount }} 页</span>
              <ElButton data-test="batch-next" round :disabled="batchPage === pageCount" @click="batchPage++">下一页</ElButton>
            </nav>
          </section>
        </section>

        <aside class="history-section">
          <div class="history-heading">
            <div><span>已保存</span><h2>历史周报</h2></div>
            <ElIcon><Document /></ElIcon>
          </div>
          <RouterLink class="history-all-button" :to="{ name: 'weekly-report-history' }">按年份、内容查询全部周报 →</RouterLink>
          <div class="history-filter">
            <label>年份<input v-model.number="year" class="period-control" data-test="history-year" type="number" min="2000" max="2100" /></label>
            <label>月份<input v-model.number="month" class="period-control" data-test="history-month" type="number" min="1" max="12" /></label>
          </div>
          <div v-if="historyPending" class="empty-history" data-test="report-history-loading" role="status">正在加载历史周报…</div>
          <div v-else-if="store.reports.length" class="report-list">
            <article v-for="report in store.reports" :key="report.id">
              <time>{{ report.weekStartDate }}</time>
              <strong>{{ report.coreWork }}</strong>
              <RouterLink
                class="action-link"
                :data-test="`report-detail-${report.id}`"
                :to="{ name: 'weekly-report-detail', params: { id: report.id } }"
              >查看详情 →</RouterLink>
            </article>
          </div>
          <div v-else class="empty-history">这个月份还没有周报。</div>
        </aside>
      </main>
    </div>
  </div>
</template>

<style scoped>
.report-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.report-shell { width: min(1440px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.report-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; }
.back-link { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 28px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.report-header p, .section-title span, .history-heading span { margin: 0 0 9px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.report-header h1 { max-width: 760px; margin: 0; font-size: clamp(34px, 4.6vw, 66px); line-height: 1.05; letter-spacing: -.06em; }
.report-header > div > span { display: block; margin-top: 15px; color: #78736b; }
.header-mark { display: grid; width: 92px; height: 92px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #17181c; background: #ffd84d; font-weight: 900; }
.report-layout { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(310px, .8fr); gap: 22px; margin-top: 42px; }
.composer-section, .history-section { padding: clamp(22px, 3vw, 34px); border-radius: 30px; background: #fbfaf7; }
.section-title h2, .history-heading h2 { margin: 0 0 24px; font-size: 28px; letter-spacing: -.04em; }
.success-notice, .error-notice { margin: 0 0 16px; padding: 12px 15px; border-radius: 15px; font-size: 13px; }
.success-notice { color: #554100; background: #fff1a8; }
.error-notice { color: #9f2d13; background: #fff0e9; }
.history-section { align-self: start; color: #fff; background: #24262d; }
.history-heading { display: flex; align-items: flex-start; justify-content: space-between; }
.history-heading :deep(.el-icon) { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 16px; color: #17181c; background: #ffd84d; font-size: 20px; }
.history-all-button { display: flex; min-height: 48px; align-items: center; justify-content: center; margin: -8px 0 18px; padding: 10px 14px; border-radius: 16px; color: #17181c; background: #ffd84d; font-size: 13px; font-weight: 800; line-height: 1.4; text-align: center; text-decoration: none; }
.history-all-button:hover { background: #ffe477; }
.history-all-button:focus-visible { outline: 3px solid #fff; outline-offset: 3px; }
.history-filter { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.history-filter label { display: grid; gap: 7px; color: #c5c5ca; font-size: 12px; font-weight: 700; }
.history-filter input { min-width: 0; min-height: 48px; padding: 0 14px; border: 1px solid #555761; border-radius: 16px; color: #fff; background: #30323a; font: inherit; font-size: 14px; }
.period-control:focus-visible { outline: 3px solid rgb(255 216 77 / 28%); outline-offset: 2px; border-color: #ffd84d; }
.report-list { display: grid; gap: 12px; margin-top: 24px; }
.report-list article { display: grid; gap: 9px; padding: 18px; border-radius: 20px; background: #30323a; }
.report-list time { color: #ffd84d; font-size: 11px; font-weight: 800; }
.report-list strong { display: -webkit-box; overflow: hidden; color: #f7f7f5; font-size: 14px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.report-list a { color: #f89a6f; font-size: 14px; font-weight: 750; text-decoration: none; }
.empty-history { margin-top: 24px; padding: 28px 16px; border: 1px dashed #4b4d55; border-radius: 20px; color: #aaaab0; font-size: 13px; text-align: center; }
.batch-panel { display: grid; gap: 14px; margin-top: 22px; padding-top: 22px; border-top: 1px solid #e1ddd5; }
.import-progress { display: grid; gap: 9px; margin-bottom: 16px; padding: 13px 16px; border-radius: 15px; color: #554100; background: #fff1a8; font-size: 13px; font-weight: 700; }
.batch-heading { position: sticky; top: 12px; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 10px 0; background: #fbfaf7; }
.batch-heading :deep(.el-button) { min-height: 46px; margin: 0; }
.batch-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; }
.batch-pagination :deep(.el-button) { min-height: 44px; margin: 0; }
.batch-heading h2 { margin: 5px 0 0; font-size: 22px; }.batch-heading span { color: #f05a18; font-size: 11px; font-weight: 800; }
.batch-notice { margin: 0; padding: 11px 14px; border-radius: 13px; color: #604900; background: #fff1a8; font-size: 12px; }
.batch-item { display: grid; gap: 12px; padding: 18px; border: 1px solid #e4dfd7; border-radius: 20px; background: #fff; }
.batch-row { display: flex; justify-content: space-between; gap: 14px; }.batch-row span { color: #8a867e; font-size: 12px; }.batch-row .saved { color: #347454; }.batch-row .failed { color: #b42318; }
.batch-item label { display: grid; gap: 6px; color: #6e6962; font-size: 12px; }.batch-item input,.batch-item textarea { padding: 10px 12px; border: 1px solid #ded9d0; border-radius: 12px; font: inherit; }.batch-item input.period-control { min-height: 48px; font-size: 14px; }.batch-item textarea { resize: vertical; }
.batch-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 940px) { .report-layout { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .report-page { padding: 0; } .report-shell { min-height: 100vh; border-radius: 0; } .header-mark { display: none; } .history-filter { grid-template-columns: 1fr; } .batch-fields { grid-template-columns: 1fr; } }
</style>
