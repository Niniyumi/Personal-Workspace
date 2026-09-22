<script setup lang="ts">
import { ChatDotRound, Document, FolderOpened, HomeFilled, Mic, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { ElAvatar, ElButton, ElIcon } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ActivityChart from '../components/ActivityChart.vue'
import YearlyWeeklyReportChart from '../components/YearlyWeeklyReportChart.vue'
import { useAuthStore } from '../features/auth/authStore'
import { useDashboardStore } from '../features/dashboard/dashboardStore'

const auth = useAuthStore()
const dashboard = useDashboardStore()
const router = useRouter()
const logoutPending = ref(false)

const totalMinutes = computed(() => Math.round((dashboard.data?.totalRecordingSeconds ?? 0) / 60))
const monthMinutes = computed(() => Math.round((dashboard.data?.monthRecordingSeconds ?? 0) / 60))
const recentTarget = (item: { type: string; id: number }) => item.type === 'COURSE'
  ? { name: 'course-detail', params: { id: item.id } }
  : { name: 'weekly-report-detail', params: { id: item.id } }
const formatDate = (value: string) => new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(new Date(value))

onMounted(() => dashboard.load())

async function logout() {
  if (logoutPending.value) return
  logoutPending.value = true
  try {
    await auth.logout()
    await router.push({ name: 'login' })
  } finally {
    logoutPending.value = false
  }
}

const openWeeklyReports = () => router.push({ name: 'weekly-reports' })
const openWeeklyHistory = () => router.push({ name: 'weekly-report-history' })
const openWorkSummaries = () => router.push({ name: 'work-summaries' })
const openCourses = () => router.push({ name: 'courses' })
</script>

<template>
  <div class="page">
    <div class="shell">
      <aside class="sidebar">
        <div class="brand"><span class="logo">P</span><span>Personal Agent</span></div>
        <nav aria-label="主要导航">
          <RouterLink class="nav active" to="/"><ElIcon><HomeFilled /></ElIcon><span>工作台</span></RouterLink>
          <RouterLink class="nav" :to="{ name: 'weekly-reports' }"><ElIcon><ChatDotRound /></ElIcon><span>周报</span></RouterLink>
          <RouterLink class="nav" :to="{ name: 'courses' }"><ElIcon><Mic /></ElIcon><span>课程笔记</span></RouterLink>
          <RouterLink class="nav" :to="{ name: 'work-summaries' }"><ElIcon><FolderOpened /></ElIcon><span>工作总结</span></RouterLink>
        </nav>
        <div class="profile">
          <ElAvatar :size="72" :icon="UserFilled" :style="{ '--el-avatar-icon-size': '30px' }" aria-label="默认用户头像" />
          <div><strong>{{ auth.user?.displayName }}</strong><span>{{ auth.user?.email }}</span></div>
        </div>
      </aside>

      <main>
        <header>
          <div><p>个人工作台</p><h1>今天想整理些什么？</h1><span>你好，{{ auth.user?.displayName }}。</span></div>
          <ElButton class="standard-action-button" data-test="logout" round :icon="SwitchButton" :loading="logoutPending" @click="logout">退出登录</ElButton>
        </header>

        <div v-if="dashboard.error" class="error" role="alert">
          {{ dashboard.error }}
          <button class="standard-action-button" data-test="reload-dashboard" type="button" @click="dashboard.load">重新加载</button>
        </div>

        <p v-if="dashboard.loading" class="loading-notice" data-test="dashboard-loading" role="status">正在加载工作台…</p>

        <section class="grid" :class="{ loading: dashboard.loading }" :aria-busy="dashboard.loading">
          <article class="trend card">
            <div class="heading"><div><span class="eyebrow">近六个月</span><h2>内容记录趋势</h2></div><div class="legend"><span><i class="orange"></i>周报</span><span><i class="yellow"></i>笔记</span></div></div>
            <ActivityChart :points="dashboard.data?.monthlyActivity ?? []" />
          </article>

          <article class="month-card">
            <span>本月概览</span><h2>{{ dashboard.data?.monthWeeklyReports ?? 0 }} 份周报</h2>
            <dl>
              <div><dt>课程笔记</dt><dd>{{ dashboard.data?.monthCourseNotes ?? 0 }} 篇</dd></div>
              <div><dt>录音时长</dt><dd>{{ monthMinutes }} 分钟</dd></div>
            </dl>
          </article>

          <article class="feature card">
            <div class="icon orange-bg"><ElIcon><Document /></ElIcon></div>
            <span class="eyebrow">周报</span><h2>{{ dashboard.data?.totalWeeklyReports ?? 0 }} 份周报</h2>
            <p>近三年整理情况</p>
            <YearlyWeeklyReportChart :points="dashboard.data?.yearlyWeeklyReports ?? []" />
            <div class="actions">
              <ElButton class="standard-action-button" data-test="open-weekly-reports" round @click="openWeeklyReports">写周报</ElButton>
              <ElButton class="standard-action-button" data-test="open-weekly-history" round @click="openWeeklyHistory">查看周报</ElButton>
              <ElButton class="standard-action-button" data-test="open-work-summaries" round @click="openWorkSummaries">查看总结</ElButton>
            </div>
          </article>

          <article class="feature card">
            <div class="icon yellow-bg"><ElIcon><Mic /></ElIcon></div>
            <span class="eyebrow">课程笔记</span><h2>{{ dashboard.data?.totalCourseNotes ?? 0 }} 篇笔记</h2>
            <p>已保存 {{ totalMinutes }} 分钟录音，可随时回听原始内容。</p>
            <ElButton class="standard-action-button" data-test="open-courses" round @click="openCourses">查看课程</ElButton>
          </article>

          <article class="recent card">
            <div class="heading"><h2>最近更新</h2></div>
            <p v-if="!dashboard.data?.recentItems.length" class="empty">还没有内容，先写一份周报或录一节课吧。</p>
            <RouterLink v-for="item in dashboard.data?.recentItems" :key="`${item.type}-${item.id}`" class="recent-item interactive-row" :to="recentTarget(item)">
              <i></i><div><strong>{{ item.title }}</strong><span>{{ item.type === 'COURSE' ? '课程笔记' : '周报' }}</span></div><time>{{ formatDate(item.updatedAt) }}</time>
            </RouterLink>
          </article>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page{min-height:100vh;padding:clamp(16px,3vw,42px);color:#17181c;background:#d8d5cf;--el-color-primary:#17181c}.shell{display:grid;width:min(1480px,100%);min-height:calc(100vh - 84px);margin:auto;grid-template-columns:230px minmax(0,1fr);overflow:hidden;border-radius:32px;background:#f5f2ec;box-shadow:0 28px 65px rgb(35 32 27 / 14%)}.sidebar{display:flex;flex-direction:column;padding:30px 22px 24px;border-right:1px solid #e1ddd5;background:#fbfaf7}.brand{display:flex;align-items:center;gap:11px;padding:0 8px;font-weight:800}.logo{display:grid;width:38px;height:38px;place-items:center;border-radius:14px;background:#ffd84d}.sidebar nav{display:grid;gap:8px;margin-top:52px}.nav{display:flex;min-height:48px;align-items:center;gap:12px;padding:0 16px;border-radius:18px;color:#66645f;text-decoration:none}.nav.active{color:#17181c;background:#e7e3dc;font-weight:750}.profile{display:flex;align-items:center;gap:10px;margin-top:auto}.profile :deep(.el-avatar){color:#fff;background:#f05a18}.profile div{min-width:0}.profile strong,.profile span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.profile span{margin-top:3px;color:#88857e;font-size:11px}main{min-width:0;padding:38px clamp(24px,4vw,54px) 44px}header,.heading{display:flex;align-items:flex-start;justify-content:space-between;gap:20px}header p{margin:0 0 8px;color:#f05a18;font-size:12px;font-weight:800}header h1{margin:0;font-size:clamp(30px,3.4vw,48px);letter-spacing:-.055em}header span{display:block;margin-top:9px;color:#77746e}.error{margin-top:24px;padding:12px 16px;border-radius:12px;color:#8c2e0a;background:#fff1eb}.error button{border:0;color:inherit;background:transparent;text-decoration:underline;cursor:pointer}.grid{display:grid;margin-top:34px;grid-template-columns:repeat(12,minmax(0,1fr));gap:18px;transition:opacity .2s}.grid.loading{opacity:.55}.card{padding:28px;border-radius:26px;background:#fff}.trend{grid-column:span 8}.trend h2,.recent h2{margin:7px 0 22px;font-size:27px}.eyebrow{color:#8a867e;font-size:11px;font-weight:800}.legend{display:flex;gap:14px;color:#77746e;font-size:12px}.legend span{display:flex;align-items:center;gap:6px}.legend i{width:9px;height:9px;border-radius:3px}.orange{background:#f05a18}.yellow{background:#ffd84d}.month-card{grid-column:span 4;padding:28px;border-radius:26px;color:#fff;background:#24262d}.month-card>span{color:#b9bac0;font-size:13px}.month-card h2{margin:10px 0 34px;font-size:34px}.month-card dl{margin:0}.month-card dl div{display:flex;justify-content:space-between;padding:14px 0;border-top:1px solid #3b3d45}.month-card dt{color:#b9bac0}.month-card dd{margin:0;font-weight:750}.feature{grid-column:span 4;min-height:225px}.icon{display:grid;width:48px;height:48px;place-items:center;margin-bottom:24px;border-radius:17px;font-size:22px}.orange-bg{color:#fff;background:#f05a18}.yellow-bg{background:#ffd84d}.feature h2{margin:8px 0;font-size:25px}.feature p{min-height:46px;color:#74716a;line-height:1.65}.actions{display:flex;flex-wrap:wrap;gap:8px}.actions :deep(.el-button){margin:0}.recent{grid-column:span 4}.recent-item{display:grid;min-height:58px;align-items:center;grid-template-columns:8px minmax(0,1fr) auto;gap:11px;border-top:1px solid #eeece7;color:inherit;text-decoration:none}.recent-item>i{width:7px;height:7px;border-radius:50%;background:#f05a18}.recent-item strong,.recent-item span{display:block}.recent-item span,.recent-item time,.empty{margin-top:3px;color:#8d8981;font-size:11px}.empty{line-height:1.7}@media(max-width:1000px){.shell{grid-template-columns:86px minmax(0,1fr)}.sidebar{align-items:center;padding-inline:14px}.brand>span:last-child,.nav span,.profile div{display:none}.sidebar nav{width:100%}.nav{justify-content:center;padding:0}.trend,.month-card{grid-column:span 12}.feature,.recent{grid-column:span 6}}@media(max-width:700px){.page{padding:0}.shell{display:block;min-height:100vh;border-radius:0}.sidebar{display:none}main{padding:24px 16px}header{align-items:flex-end}.feature,.recent{grid-column:span 12}.card,.month-card{padding:22px}}
.error button.standard-action-button {
  min-height: 44px;
  margin-left: 8px;
  padding: 0 14px;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-weight: 700;
  text-decoration: none;
}
.loading-notice { margin: 24px 0 -14px; color: #77746e; font-size: 13px; }
</style>
