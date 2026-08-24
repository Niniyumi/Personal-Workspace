<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { DocumentAdd, Upload } from '@element-plus/icons-vue'
import { ElButton, ElIcon } from 'element-plus'
import type { DocxImportResult, WeeklyReportInput } from '../features/weeklyReport/types'

const props = withDefaults(defineProps<{
  initialValue?: Partial<WeeklyReportInput>
  imported?: DocxImportResult | null
  loading?: boolean
  submitLabel?: string
}>(), {
  initialValue: () => ({}),
  imported: null,
  loading: false,
  submitLabel: '保存周报',
})

const emit = defineEmits<{
  save: [value: WeeklyReportInput]
  import: [file: File]
}>()

const validationMessage = ref('')

function currentMonday() {
  const date = new Date()
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  return date.toLocaleDateString('en-CA')
}

const form = reactive<WeeklyReportInput>({
  weekStartDate: currentMonday(),
  coreWork: '',
  problems: '',
  nextWeekPlan: '',
  sourceFileName: null,
})

watch(
  () => props.initialValue,
  (value) => {
    form.weekStartDate = value.weekStartDate || currentMonday()
    form.coreWork = value.coreWork || ''
    form.problems = value.problems || ''
    form.nextWeekPlan = value.nextWeekPlan || ''
    form.sourceFileName = value.sourceFileName || null
  },
  { deep: true, immediate: true },
)

function appendText(current: string, incoming: string | null) {
  if (!incoming?.trim()) return current
  return current.trim() ? `${current.trimEnd()}\n${incoming.trim()}` : incoming.trim()
}

watch(
  () => props.imported,
  (value) => {
    if (!value) return
    // 导入内容追加到用户已输入的文字之后，避免文档识别结果覆盖在线补充内容。
    form.coreWork = appendText(form.coreWork, value.coreWork)
    form.problems = appendText(form.problems, value.problems)
    form.nextWeekPlan = appendText(form.nextWeekPlan, value.nextWeekPlan)
    form.sourceFileName = value.sourceFileName
  },
)

function selectFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) emit('import', file)
  input.value = ''
}

function submit() {
  validationMessage.value = ''
  if (!form.coreWork.trim()) {
    validationMessage.value = '请填写本周核心工作'
    return
  }
  emit('save', {
    weekStartDate: form.weekStartDate,
    coreWork: form.coreWork.trim(),
    problems: form.problems.trim(),
    nextWeekPlan: form.nextWeekPlan.trim(),
    sourceFileName: form.sourceFileName,
  })
}
</script>

<template>
  <form class="weekly-form" @submit.prevent="submit">
    <div class="form-toolbar">
      <label class="week-field">
        <span>周开始日期</span>
        <input v-model="form.weekStartDate" type="date" required />
      </label>
      <label class="upload-button">
        <ElIcon><Upload /></ElIcon>
        <span>{{ loading ? '处理中…' : '导入 DOCX' }}</span>
        <input type="file" accept=".docx" :disabled="loading" @change="selectFile" />
      </label>
    </div>

    <p v-if="form.sourceFileName" class="source-file">
      <ElIcon><DocumentAdd /></ElIcon> 已导入 {{ form.sourceFileName }}
    </p>

    <label class="editor-card featured">
      <span class="field-index">01 / REQUIRED</span>
      <strong>本周核心工作</strong>
      <textarea
        v-model="form.coreWork"
        data-test="core-work"
        rows="7"
        placeholder="写下本周完成的重点任务、结果和产出……"
      ></textarea>
    </label>

    <div class="secondary-fields">
      <label class="editor-card">
        <span class="field-index">02 / OPTIONAL</span>
        <strong>遇到的问题</strong>
        <textarea
          v-model="form.problems"
          data-test="problems"
          rows="5"
          placeholder="记录阻塞、风险或需要协助的事项（选填）"
        ></textarea>
      </label>
      <label class="editor-card">
        <span class="field-index">03 / OPTIONAL</span>
        <strong>下周工作计划</strong>
        <textarea
          v-model="form.nextWeekPlan"
          data-test="next-week-plan"
          rows="5"
          placeholder="写下下周准备推进的工作（选填）"
        ></textarea>
      </label>
    </div>

    <div class="form-footer">
      <p v-if="validationMessage" role="alert">{{ validationMessage }}</p>
      <span v-else>DOCX 识别结果会追加到已有文字后面，你仍可继续修改。</span>
      <ElButton native-type="submit" type="primary" round :loading="loading">
        {{ submitLabel }}
      </ElButton>
    </div>
  </form>
</template>

<style scoped>
.weekly-form { display: grid; gap: 18px; }
.form-toolbar { display: flex; align-items: end; justify-content: space-between; gap: 16px; }
.week-field { display: grid; gap: 7px; color: #66615a; font-size: 12px; font-weight: 750; }
.week-field input { min-height: 44px; padding: 0 16px; border: 1px solid #ded9d0; border-radius: 22px; color: #17181c; background: #fff; }
.upload-button { display: flex; min-height: 44px; align-items: center; gap: 8px; padding: 0 18px; border: 1px solid #d8d3ca; border-radius: 22px; color: #17181c; background: #fff; font-size: 13px; font-weight: 750; cursor: pointer; }
.upload-button:hover { border-color: #f05a18; color: #e84f12; }
.upload-button input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.source-file { display: flex; align-items: center; gap: 7px; margin: 0; color: #7b4a16; font-size: 12px; }
.editor-card { display: grid; gap: 8px; padding: 22px; border: 1px solid #e5e1d9; border-radius: 24px; background: #fff; }
.editor-card.featured { border-top: 5px solid #f05a18; }
.field-index { color: #f05a18; font-size: 10px; font-weight: 850; letter-spacing: .12em; }
.editor-card strong { font-size: 18px; }
.editor-card textarea { width: 100%; padding: 14px 0 0; border: 0; border-top: 1px solid #eeeae4; outline: 0; color: #252525; background: transparent; font: inherit; line-height: 1.75; resize: vertical; }
.editor-card textarea::placeholder { color: #aaa59c; }
.secondary-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.form-footer { display: flex; min-height: 48px; align-items: center; justify-content: space-between; gap: 20px; }
.form-footer span, .form-footer p { margin: 0; color: #817c73; font-size: 12px; }
.form-footer p { color: #b42318; font-weight: 700; }
.form-footer :deep(.el-button) { min-width: 138px; min-height: 46px; padding-inline: 24px; --el-button-bg-color: #17181c; --el-button-border-color: #17181c; --el-button-hover-bg-color: #f05a18; --el-button-hover-border-color: #f05a18; }
@media (max-width: 720px) {
  .form-toolbar, .form-footer { align-items: stretch; flex-direction: column; }
  .secondary-fields { grid-template-columns: 1fr; }
  .upload-button { justify-content: center; }
  .form-footer :deep(.el-button) { width: 100%; }
}
</style>
