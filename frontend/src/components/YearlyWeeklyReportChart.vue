<script setup lang="ts">
import { computed } from 'vue'
import type { YearlyWeeklyReports } from '../features/dashboard/types'

const props = defineProps<{ points: YearlyWeeklyReports[] }>()
const maximum = computed(() => Math.max(1, ...props.points.map(point => point.count)))
</script>

<template>
  <div class="yearly-chart" aria-label="近三年周报数量">
    <div v-for="point in points" :key="point.year" class="year-column">
      <strong>{{ point.count }} 份</strong>
      <div class="bar-track"><span :style="{ height: `${point.count / maximum * 100}%` }"></span></div>
      <span>{{ point.year }} 年</span>
    </div>
  </div>
</template>

<style scoped>
.yearly-chart { display: grid; height: 155px; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin: 18px 0 22px; }
.year-column { display: grid; min-width: 0; grid-template-rows: auto 1fr auto; gap: 6px; color: #77736c; font-size: 12px; text-align: center; }
.year-column strong { color: #17181c; font-size: 13px; }
.bar-track { display: flex; min-height: 0; align-items: end; justify-content: center; border-bottom: 1px solid #e9e5de; }
.bar-track span { width: min(46px, 70%); min-height: 2px; border-radius: 8px 8px 0 0; background: #f05a18; }
</style>
