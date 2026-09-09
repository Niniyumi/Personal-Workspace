<script setup lang="ts">
import { computed } from 'vue'
import type { MonthlyActivity } from '../features/dashboard/types'

const props = defineProps<{ points: MonthlyActivity[] }>()
const maximum = computed(() => Math.max(1, ...props.points.flatMap((point) => [point.weeklyReports, point.courseNotes])))
const height = (value: number) => `${Math.max(value === 0 ? 2 : 10, value / maximum.value * 100)}%`
const monthLabel = (month: string) => `${Number(month.slice(5))}月`
</script>

<template>
  <div class="chart" aria-label="近六个月内容数量">
    <div v-for="point in points" :key="point.month" class="column">
      <div class="bars">
        <span class="bar report" :style="{ height: height(point.weeklyReports) }" :title="`${point.weeklyReports} 份周报`"></span>
        <span class="bar note" :style="{ height: height(point.courseNotes) }" :title="`${point.courseNotes} 篇笔记`"></span>
      </div>
      <span>{{ monthLabel(point.month) }}</span>
    </div>
  </div>
</template>

<style scoped>
.chart { display: grid; height: 220px; grid-template-columns: repeat(6, minmax(42px, 1fr)); gap: 12px; align-items: end; }
.column { display: grid; height: 100%; grid-template-rows: 1fr auto; gap: 10px; color: #817d75; font-size: 12px; text-align: center; }
.bars { display: flex; height: 100%; align-items: end; justify-content: center; gap: 6px; border-bottom: 1px solid #e9e5de; }
.bar { display: block; width: min(22px, 36%); min-height: 2px; border-radius: 8px 8px 2px 2px; transition: height .25s ease; }
.report { background: #f05a18; }
.note { background: #ffd84d; }
@media (max-width: 600px) { .chart { gap: 4px; height: 180px; } .bars { gap: 3px; } }
</style>
