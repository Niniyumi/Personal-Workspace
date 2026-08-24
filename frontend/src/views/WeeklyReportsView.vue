<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { ElIcon } from 'element-plus'
import WeeklyReportForm from '../components/WeeklyReportForm.vue'
import { useWeeklyReportStore } from '../features/weeklyReport/weeklyReportStore'
import type { WeeklyReportInput } from '../features/weeklyReport/types'

const store = useWeeklyReportStore()
const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const notice = ref('')

onMounted(() => {
  store.imported = null
  void loadHistory()
})

watch([year, month], ([nextYear, nextMonth]) => {
  void loadHistory(nextYear, nextMonth)
})

async function loadHistory(nextYear = year.value, nextMonth = month.value) {
  try {
    await store.loadMonth(nextYear, nextMonth)
  } catch {
    // Store 已统一提供面向用户的错误信息，页面只需阻止未处理的 Promise。
  }
}

async function save(input: WeeklyReportInput) {
  try {
    const created = await store.create(input)
    if (!created) return
    notice.value = '周报已保存'
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
  }
}

async function importFile(file: File) {
  notice.value = ''
  try {
    await store.importDocx(file)
  } catch {
    // 错误信息由 Store 展示在表单上方。
  }
}
</script>

<template>
  <div class="report-page">
    <div class="report-shell">
      <header class="report-header">
        <div>
          <RouterLink class="back-link" :to="{ name: 'home' }"><ElIcon><ArrowLeft /></ElIcon> 返回工作台</RouterLink>
          <p>WEEKLY REPORT AGENT</p>
          <h1>把这一周，整理成清晰的记录。</h1>
          <span>在线补充文字，或导入 DOCX 让 Agent 帮你归类。</span>
        </div>
        <div class="header-mark">W / 01</div>
      </header>

      <main class="report-layout">
        <section class="composer-section">
          <div class="section-title"><span>NEW REPORT</span><h2>新建周报</h2></div>
          <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>
          <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
          <WeeklyReportForm
            :imported="store.imported"
            :loading="store.loading"
            @save="save"
            @import="importFile"
          />
        </section>

        <aside class="history-section">
          <div class="history-heading">
            <div><span>ARCHIVE</span><h2>历史周报</h2></div>
            <ElIcon><Document /></ElIcon>
          </div>
          <div class="history-filter">
            <label>年份<input v-model.number="year" data-test="history-year" type="number" min="2000" max="2100" /></label>
            <label>月份<input v-model.number="month" data-test="history-month" type="number" min="1" max="12" /></label>
          </div>
          <div v-if="store.reports.length" class="report-list">
            <article v-for="report in store.reports" :key="report.id">
              <time>{{ report.weekStartDate }}</time>
              <strong>{{ report.coreWork }}</strong>
              <RouterLink
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
.history-filter { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.history-filter label { display: grid; gap: 7px; color: #aaaab0; font-size: 11px; }
.history-filter input { min-width: 0; min-height: 42px; padding: 0 12px; border: 1px solid #44464f; border-radius: 16px; color: #fff; background: #30323a; }
.report-list { display: grid; gap: 12px; margin-top: 24px; }
.report-list article { display: grid; gap: 9px; padding: 18px; border-radius: 20px; background: #30323a; }
.report-list time { color: #ffd84d; font-size: 11px; font-weight: 800; }
.report-list strong { display: -webkit-box; overflow: hidden; color: #f7f7f5; font-size: 14px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.report-list a { color: #f89a6f; font-size: 12px; font-weight: 750; text-decoration: none; }
.empty-history { margin-top: 24px; padding: 28px 16px; border: 1px dashed #4b4d55; border-radius: 20px; color: #aaaab0; font-size: 13px; text-align: center; }
@media (max-width: 940px) { .report-layout { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .report-page { padding: 0; } .report-shell { min-height: 100vh; border-radius: 0; } .header-mark { display: none; } }
</style>
