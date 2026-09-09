<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElIcon } from 'element-plus'
import { useRoute } from 'vue-router'
import WeeklyReportForm from '../components/WeeklyReportForm.vue'
import { useWeeklyReportStore } from '../features/weeklyReport/weeklyReportStore'
import type { WeeklyReportInput } from '../features/weeklyReport/types'

const route = useRoute()
const store = useWeeklyReportStore()
const reportId = Number(route.params.id)
const initialValue = ref<Partial<WeeklyReportInput>>({})
const notice = ref('')

onMounted(async () => {
  store.imported = null
  try {
    const report = await store.loadOne(reportId)
    if (!report) return
    initialValue.value = {
      weekStartDate: report.weekStartDate,
      coreWork: report.coreWork,
      problems: report.problems || '',
      nextWeekPlan: report.nextWeekPlan || '',
      sourceFileName: report.sourceFileName,
    }
  } catch {
    // Store 已记录接口错误，页面保留空表单供用户返回列表。
  }
})

async function save(input: WeeklyReportInput) {
  try {
    const updated = await store.update(reportId, input)
    if (updated) notice.value = '修改已保存'
  } catch {
    notice.value = ''
  }
}

async function importFile(files: File[]) {
  notice.value = ''
  try {
    if (files[0]) await store.importDocx(files[0])
  } catch {
    // 错误信息由 Store 展示在表单上方。
  }
}
</script>

<template>
  <div class="detail-page">
    <main class="detail-shell">
      <RouterLink class="back-link" :to="{ name: 'weekly-reports' }"><ElIcon><ArrowLeft /></ElIcon> 返回周报列表</RouterLink>
      <header>
        <p>周报 #{{ reportId }}</p>
        <h1>编辑周报</h1>
      </header>
      <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>
      <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
      <WeeklyReportForm
        :initial-value="initialValue"
        :imported="store.imported"
        :loading="store.loading"
        submit-label="保存修改"
        @save="save"
        @import="importFile"
      />
    </main>
  </div>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: clamp(18px, 4vw, 54px); color: #17181c; background: #d8d5cf; }
.detail-shell { width: min(1040px, 100%); margin: 0 auto; padding: clamp(24px, 5vw, 58px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.back-link { display: inline-flex; align-items: center; gap: 7px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
header { margin: 34px 0; }
header p { margin: 0 0 10px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
header h1 { margin: 0; font-size: clamp(32px, 5vw, 56px); letter-spacing: -.055em; }
.success-notice, .error-notice { margin: 0 0 16px; padding: 12px 15px; border-radius: 15px; font-size: 13px; }
.success-notice { color: #554100; background: #fff1a8; }
.error-notice { color: #9f2d13; background: #fff0e9; }
@media (max-width: 620px) { .detail-page { padding: 0; } .detail-shell { min-height: 100vh; border-radius: 0; } }
</style>
