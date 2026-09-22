<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElButton, ElIcon } from 'element-plus'
import { useWeeklyReportStore } from '../features/weeklyReport/weeklyReportStore'

const store = useWeeklyReportStore()
const year = ref(new Date().getFullYear())
const month = ref(0)
const keyword = ref('')
const appliedKeyword = ref('')
const page = ref(1)
const pending = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil((store.searchResult?.total ?? 0) / 10)))
let searchTimer: ReturnType<typeof setTimeout> | undefined
let requestGeneration = 0

async function load() {
  const generation = ++requestGeneration
  pending.value = true
  try {
    await store.search(year.value, month.value, appliedKeyword.value, page.value)
  } catch {
    // Store 提供错误提示，页面保留当前筛选条件以便重试。
  } finally {
    if (generation === requestGeneration) pending.value = false
  }
}

watch(keyword, value => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { appliedKeyword.value = value.trim() }, 350)
})
watch([year, month, appliedKeyword], () => {
  if (page.value !== 1) page.value = 1
  else void load()
})
watch(page, () => { void load() })
onMounted(() => { void load() })
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })

function formatSavedAt(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<template>
  <div class="history-page">
    <main class="history-shell">
      <RouterLink class="back-link" :to="{ name: 'home' }"><ElIcon><ArrowLeft /></ElIcon> 返回工作台</RouterLink>
      <header class="page-heading">
        <div><p>周报归档</p><h1>查询周报</h1><span>按所属周筛选，搜索已保存的工作内容。</span></div>
        <RouterLink class="new-report" :to="{ name: 'weekly-reports' }">写周报</RouterLink>
      </header>

      <section class="history-card">
        <div class="filters">
          <label>年份<input v-model.number="year" data-test="search-year" type="number" min="2000" max="2100" /></label>
          <label>月份<select v-model.number="month" data-test="search-month">
            <option :value="0">全年</option>
            <option v-for="value in 12" :key="value" :value="value">{{ value }} 月</option>
          </select></label>
          <label class="keyword-field">内容搜索<input v-model="keyword" data-test="search-keyword" type="search" placeholder="搜索核心工作、问题或计划" /></label>
        </div>
        <p v-if="store.error" class="error-notice" role="alert">{{ store.error }} <ElButton round @click="load">重试</ElButton></p>
        <p v-if="pending" class="loading-notice" role="status">正在查询周报…</p>
        <p v-else class="result-count">找到 {{ store.searchResult?.total ?? 0 }} 份周报</p>
        <div class="table-scroll">
          <table v-if="!pending && !store.error && store.searchResult?.items.length" class="report-table">
            <thead><tr><th>所属周</th><th>保存时间</th><th>字数</th><th>来源</th><th>内容预览</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="report in store.searchResult.items" :key="report.id" data-test="history-row">
                <td>{{ report.weekStartDate }}</td>
                <td>{{ formatSavedAt(report.createdAt) }}</td>
                <td>{{ report.characterCount }}</td>
                <td>{{ report.sourceFileName || '在线填写' }}</td>
                <td class="preview">{{ report.preview }}</td>
                <td><RouterLink :to="{ name: 'weekly-report-detail', params: { id: report.id } }">查看详情 →</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <p v-else-if="!pending && !store.error" class="empty-result">没有找到符合条件的周报。</p>
        </div>
        <nav v-if="(store.searchResult?.total ?? 0) > 10" class="pagination" aria-label="周报分页">
          <ElButton data-test="history-prev" round :disabled="page === 1 || pending" @click="page--">上一页</ElButton>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <ElButton data-test="history-next" round :disabled="page >= totalPages || pending" @click="page++">下一页</ElButton>
        </nav>
      </section>
    </main>
  </div>
</template>

<style scoped>
.history-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.history-shell { width: min(1440px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; }
.back-link { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 28px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.page-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; }
.page-heading p { margin: 0 0 8px; color: #f05a18; font-size: 12px; font-weight: 800; }
.page-heading h1 { margin: 0; font-size: clamp(34px, 4.6vw, 64px); letter-spacing: -.05em; }
.page-heading span { display: block; margin-top: 12px; color: #77736c; }
.new-report { display: inline-flex; min-height: 46px; align-items: center; padding: 0 22px; border-radius: 23px; color: #fff; background: #17181c; font-weight: 750; text-decoration: none; white-space: nowrap; }
.new-report:hover { background: #f05a18; }
.history-card { margin-top: 34px; padding: clamp(22px, 3vw, 34px); border-radius: 28px; background: #fff; }
.filters { display: grid; grid-template-columns: 150px 150px minmax(220px, 1fr); gap: 14px; }
.filters label { display: grid; gap: 7px; color: #66615a; font-size: 13px; font-weight: 700; }
.filters input, .filters select { min-width: 0; min-height: 48px; padding: 0 14px; border: 1px solid #ded9d0; border-radius: 16px; color: #17181c; background: #f7f4ef; font: inherit; }
.filters input:focus-visible, .filters select:focus-visible, .new-report:focus-visible { outline: 3px solid rgb(240 90 24 / 28%); outline-offset: 2px; }
.result-count, .loading-notice { margin: 24px 0 12px; color: #77736c; font-size: 13px; }
.error-notice { margin: 18px 0; padding: 12px 16px; border-radius: 14px; color: #9f2d13; background: #fff0e9; }
.table-scroll { overflow-x: auto; }
.report-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.report-table th, .report-table td { padding: 16px 12px; border-bottom: 1px solid #eeeae4; text-align: left; vertical-align: top; }
.report-table th { color: #77736c; font-size: 12px; white-space: nowrap; }
.report-table td:first-child, .report-table td:nth-child(3) { white-space: nowrap; }
.report-table td:nth-child(2) { min-width: 140px; }
.report-table td:nth-child(4) { max-width: 155px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.report-table .preview { min-width: 220px; max-width: 420px; line-height: 1.55; }
.report-table a { color: #d94914; font-weight: 750; text-decoration: none; white-space: nowrap; }
.empty-result { padding: 44px 12px; color: #817c73; text-align: center; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 22px; }
.pagination :deep(.el-button) { min-height: 44px; margin: 0; }
@media (max-width: 700px) { .history-page { padding: 0; } .history-shell { min-height: 100vh; border-radius: 0; } .page-heading { align-items: start; flex-direction: column; } .filters { grid-template-columns: 1fr 1fr; } .keyword-field { grid-column: 1 / -1; } }
</style>
