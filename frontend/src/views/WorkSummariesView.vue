<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'
import { ElButton, ElIcon } from 'element-plus'
import { useWorkSummaryStore } from '../features/workSummary/workSummaryStore'
import type { SummaryPeriodType, WorkSummaryInput, WorkSummarySelection } from '../features/workSummary/types'

const store = useWorkSummaryStore()
const periodType = ref<SummaryPeriodType>('QUARTER')
const year = ref(new Date().getFullYear())
const quarter = ref(Math.floor(new Date().getMonth() / 3) + 1)
const notice = ref('')
const draft = reactive<WorkSummaryInput>({ coreContent: '', routineWork: '', selfScore: 80 })

watch(
  () => store.summary,
  (summary) => {
    if (!summary) return
    draft.coreContent = summary.coreContent
    draft.routineWork = summary.routineWork
    draft.selfScore = summary.selfScore
  },
  { immediate: true },
)

function selectType(type: SummaryPeriodType) {
  periodType.value = type
  notice.value = ''
}

function selection(): WorkSummarySelection {
  if (periodType.value === 'YEAR') return { periodType: 'YEAR', year: year.value }
  return { periodType: 'QUARTER', year: year.value, quarter: quarter.value }
}

async function generate() {
  notice.value = ''
  const result = await store.generate(selection())
  if (result) notice.value = '总结已生成，你可以继续修改后保存。'
}

async function loadSaved() {
  notice.value = ''
  const result = await store.load(selection())
  if (result) notice.value = '已读取保存的总结。'
}

async function save() {
  if (!store.summary) return
  notice.value = ''
  const result = await store.update(store.summary.id, {
    coreContent: draft.coreContent.trim(),
    routineWork: draft.routineWork.trim(),
    selfScore: draft.selfScore,
  })
  if (result) notice.value = '修改已保存。'
}
</script>

<template>
  <div class="summary-page">
    <main class="summary-shell">
      <RouterLink class="back-link" :to="{ name: 'home' }"><ElIcon><ArrowLeft /></ElIcon> 返回工作台</RouterLink>
      <header class="summary-header">
        <div>
          <p>WORK REVIEW AGENT</p>
          <h1>从周报里，看见这一阶段的成长。</h1>
          <span>选择季度或年度，Agent 会整理核心成果、日常工作并给出自我评分参考。</span>
        </div>
        <div class="summary-badge"><ElIcon><MagicStick /></ElIcon></div>
      </header>

      <section class="control-panel">
        <div class="type-switch" aria-label="总结周期">
          <button
            type="button"
            :class="{ active: periodType === 'QUARTER' }"
            data-test="summary-type-quarter"
            @click="selectType('QUARTER')"
          >季度总结</button>
          <button
            type="button"
            :class="{ active: periodType === 'YEAR' }"
            data-test="summary-type-year"
            @click="selectType('YEAR')"
          >年度总结</button>
        </div>
        <label>年份<input v-model.number="year" data-test="summary-year" type="number" min="2000" max="2100" /></label>
        <label v-if="periodType === 'QUARTER'">季度
          <select v-model.number="quarter" data-test="summary-quarter">
            <option :value="1">第一季度</option><option :value="2">第二季度</option>
            <option :value="3">第三季度</option><option :value="4">第四季度</option>
          </select>
        </label>
        <div class="control-actions">
          <ElButton round :disabled="store.loading" @click="loadSaved">查看已保存</ElButton>
          <ElButton
            data-test="generate-summary"
            type="primary"
            round
            :loading="store.loading"
            @click="generate"
          >生成总结</ElButton>
        </div>
      </section>

      <p v-if="store.error" class="error-notice" role="alert">{{ store.error }}</p>
      <p v-if="notice" class="success-notice" role="status">{{ notice }}</p>

      <section v-if="store.summary" class="summary-result">
        <div class="result-heading">
          <div><span>GENERATED RESULT</span><h2>{{ store.summary.periodStart }} — {{ store.summary.periodEnd }}</h2></div>
          <strong>{{ draft.selfScore }}</strong>
        </div>
        <div class="result-grid">
          <label class="result-card core-card">
            <span>01 / KEY RESULTS</span><strong>核心内容</strong>
            <textarea v-model="draft.coreContent" data-test="summary-core" rows="8"></textarea>
          </label>
          <label class="result-card">
            <span>02 / ROUTINE</span><strong>日常工作</strong>
            <textarea v-model="draft.routineWork" data-test="summary-routine" rows="8"></textarea>
          </label>
          <label class="score-card">
            <span>03 / SCORE</span><strong>自我评分</strong>
            <input v-model.number="draft.selfScore" data-test="summary-score" type="number" min="0" max="100" />
            <small>满分 100，可根据实际情况调整。</small>
          </label>
        </div>
        <div class="save-row">
          <span>模型生成内容只是草稿，保存前请确认内容准确。</span>
          <ElButton data-test="save-summary" type="primary" round :loading="store.saving" @click="save">保存修改</ElButton>
        </div>
      </section>
      <section v-else class="empty-result">
        <span>01</span><h2>选择时间范围后生成总结</h2><p>系统只会读取当前账号在该周期内的周报。</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.summary-page { min-height: 100vh; padding: clamp(18px, 3vw, 42px); color: #17181c; background: #d8d5cf; }
.summary-shell { width: min(1320px, 100%); min-height: calc(100vh - 84px); margin: 0 auto; padding: clamp(24px, 4vw, 54px); border-radius: 34px; background: #f6f3ed; box-shadow: 0 30px 70px rgb(35 32 27 / 14%); }
.back-link { display: inline-flex; align-items: center; gap: 7px; color: #57534d; font-size: 13px; font-weight: 750; text-decoration: none; }
.summary-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-top: 32px; }
.summary-header p { margin: 0 0 10px; color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.summary-header h1 { max-width: 820px; margin: 0; font-size: clamp(34px, 5vw, 64px); line-height: 1.05; letter-spacing: -.06em; }
.summary-header span { display: block; margin-top: 15px; color: #78736b; }
.summary-badge { display: grid; width: 92px; height: 92px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #fff; background: #f05a18; font-size: 30px; }
.control-panel { display: flex; align-items: end; gap: 14px; margin-top: 38px; padding: 20px; border-radius: 26px; background: #fff; }
.type-switch { display: flex; padding: 4px; border-radius: 22px; background: #ece8e1; }
.type-switch button { min-height: 40px; padding: 0 17px; border: 0; border-radius: 20px; color: #6f6b64; background: transparent; cursor: pointer; }
.type-switch button.active { color: #fff; background: #17181c; }
.control-panel label { display: grid; gap: 6px; color: #77736c; font-size: 11px; }
.control-panel input, .control-panel select { min-height: 44px; padding: 0 13px; border: 1px solid #ded9d0; border-radius: 16px; color: #17181c; background: #f7f4ef; }
.control-actions { display: flex; gap: 10px; margin-left: auto; }
.control-actions :deep(.el-button--primary), .save-row :deep(.el-button--primary) { --el-button-bg-color: #17181c; --el-button-border-color: #17181c; --el-button-hover-bg-color: #f05a18; --el-button-hover-border-color: #f05a18; }
.error-notice, .success-notice { margin: 18px 0 0; padding: 13px 16px; border-radius: 16px; font-size: 13px; }
.error-notice { color: #9f2d13; background: #fff0e9; }
.success-notice { color: #554100; background: #fff1a8; }
.summary-result, .empty-result { margin-top: 22px; padding: clamp(22px, 3vw, 34px); border-radius: 30px; background: #fbfaf7; }
.result-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.result-heading span, .result-card > span, .score-card > span { color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .13em; }
.result-heading h2 { margin: 8px 0 0; font-size: 24px; }
.result-heading > strong { display: grid; width: 74px; height: 74px; place-items: center; border-radius: 50%; background: #ffd84d; font-size: 28px; }
.result-grid { display: grid; grid-template-columns: 1fr 1fr 240px; gap: 16px; margin-top: 25px; }
.result-card, .score-card { display: grid; align-content: start; gap: 9px; padding: 22px; border: 1px solid #e2ddd5; border-radius: 23px; background: #fff; }
.result-card.core-card { border-top: 5px solid #f05a18; }
.result-card strong, .score-card strong { font-size: 18px; }
.result-card textarea { width: 100%; padding-top: 13px; border: 0; border-top: 1px solid #eeeae4; outline: 0; font: inherit; line-height: 1.7; resize: vertical; }
.score-card { color: #fff; background: #24262d; }
.score-card input { width: 100%; padding: 10px 0; border: 0; border-bottom: 1px solid #50525a; outline: 0; color: #ffd84d; background: transparent; font-size: 54px; font-weight: 850; }
.score-card small { color: #aaaab0; line-height: 1.5; }
.save-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 20px; }
.save-row > span { color: #817c73; font-size: 12px; }
.empty-result { min-height: 270px; text-align: center; }
.empty-result > span { display: grid; width: 74px; height: 74px; margin: 20px auto; place-items: center; border-radius: 50%; background: #ffd84d; font-size: 24px; font-weight: 850; }
.empty-result h2 { margin: 0; }.empty-result p { color: #817c73; }
@media (max-width: 980px) { .control-panel { align-items: stretch; flex-wrap: wrap; } .control-actions { width: 100%; margin-left: 0; } .result-grid { grid-template-columns: 1fr 1fr; } .score-card { grid-column: span 2; } }
@media (max-width: 650px) { .summary-page { padding: 0; } .summary-shell { min-height: 100vh; border-radius: 0; } .summary-badge { display: none; } .type-switch { width: 100%; } .type-switch button { flex: 1; } .control-actions { display: grid; } .result-grid { grid-template-columns: 1fr; } .score-card { grid-column: auto; } .save-row { align-items: stretch; flex-direction: column; } }
</style>
