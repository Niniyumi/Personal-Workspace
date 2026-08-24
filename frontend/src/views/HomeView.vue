<script setup lang="ts">
import { computed } from 'vue'
import {
  ChatDotRound,
  Document,
  FolderOpened,
  HomeFilled,
  Mic,
  Promotion,
  Search,
  SwitchButton,
  Upload,
} from '@element-plus/icons-vue'
import { ElAvatar, ElButton, ElIcon, ElInput, ElTag } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore'

const store = useAuthStore()
const router = useRouter()

const avatarText = computed(() => store.user?.displayName.trim().slice(0, 1) || 'U')

const recentItems = [
  { title: '账号与登录', type: '已完成', time: '当前阶段' },
  { title: '结构化周报', type: '准备开发', time: '下一阶段' },
  { title: '课程笔记', type: '规划完成', time: '后续阶段' },
]

async function logout() {
  await store.logout()
  await router.push({ name: 'login' })
}

function openWeeklyReports() {
  void router.push({ name: 'weekly-reports' })
}

function openWorkSummaries() {
  void router.push({ name: 'work-summaries' })
}
</script>

<template>
  <div class="workspace-page">
    <div class="workspace-shell">
      <aside class="workspace-sidebar">
        <div class="workspace-brand"><span class="workspace-logo">P</span><span>Personal Agent</span></div>
        <nav aria-label="主要导航">
          <a class="workspace-nav active" href="#workspace"><ElIcon><HomeFilled /></ElIcon><span>工作台</span></a>
          <button class="workspace-nav nav-button" type="button" @click="openWeeklyReports"><ElIcon><ChatDotRound /></ElIcon><span>周报助手</span></button>
          <a class="workspace-nav" href="#course"><ElIcon><Mic /></ElIcon><span>课程笔记</span></a>
          <a class="workspace-nav" href="#archive"><ElIcon><FolderOpened /></ElIcon><span>资料归档</span></a>
        </nav>
        <div class="sidebar-spacer"></div>
        <div class="workspace-profile">
          <ElAvatar :size="42">{{ avatarText }}</ElAvatar>
          <div><strong>{{ store.user?.displayName }}</strong><span>{{ store.user?.email }}</span></div>
        </div>
      </aside>

      <main id="workspace" class="workspace-main">
        <header class="workspace-header">
          <div>
            <p>PERSONAL WORKSPACE</p>
            <h1>今天想整理些什么？</h1>
            <span>你好，{{ store.user?.displayName }}。把零散的文字、录音和文档交给你的个人 Agent。</span>
          </div>
          <div class="header-actions">
            <ElInput class="search-input" placeholder="搜索你的资料" :prefix-icon="Search" disabled />
            <ElButton data-test="logout" round :icon="SwitchButton" @click="logout">退出登录</ElButton>
          </div>
        </header>

        <section class="dashboard-grid">
          <article class="agent-panel">
            <div class="panel-heading">
              <div><ElTag round effect="dark">AGENT READY</ElTag><h2>先从一段想法开始</h2></div>
              <span class="panel-number">01</span>
            </div>
            <ElInput
              type="textarea"
              :rows="4"
              resize="none"
              placeholder="例如：这周完成了登录模块，下一步准备整理周报……"
              disabled
            />
            <div class="composer-actions">
              <ElButton round :icon="Upload" disabled>上传文档</ElButton>
              <span>结构化周报正在接入，当前工作台已经连接真实账号。</span>
              <ElButton class="send-button" round type="primary" :icon="Promotion" disabled>发送</ElButton>
            </div>
          </article>

          <article class="progress-panel">
            <div class="dark-heading">
              <div><span>项目开发进度</span><strong>33%</strong></div>
              <ElTag round color="#ffd84d">PHASE 01</ElTag>
            </div>
            <div class="progress-ring"><span>1</span><small>项完成</small></div>
            <ul>
              <li><span>账号与安全</span><strong>完成</strong></li>
              <li><span>结构化周报</span><strong>准备开发</strong></li>
              <li><span>课程笔记</span><strong>后续阶段</strong></li>
            </ul>
          </article>

          <article id="weekly" class="feature-card">
            <div class="feature-icon orange"><ElIcon><Document /></ElIcon></div>
            <span class="card-index">02 / WEEKLY REPORT</span>
            <h3>结构化周报</h3>
            <p>填写核心工作、遇到的问题和下周计划，再按月份查询历史记录。</p>
            <div class="feature-actions">
              <ElButton data-test="open-weekly-reports" round @click="openWeeklyReports">开始填写</ElButton>
              <ElButton data-test="open-work-summaries" round @click="openWorkSummaries">工作总结</ElButton>
            </div>
          </article>

          <article id="course" class="feature-card">
            <div class="feature-icon yellow"><ElIcon><Mic /></ElIcon></div>
            <span class="card-index">03 / COURSE NOTES</span>
            <h3>课程笔记</h3>
            <p>录音完成后自动转写，提炼关键概念、待办事项和复习提纲。</p>
            <ElButton round disabled>后续阶段</ElButton>
          </article>

          <article id="archive" class="recent-card">
            <div class="recent-heading"><div><span>PROJECT STATUS</span><h3>最近进度</h3></div></div>
            <ul>
              <li v-for="item in recentItems" :key="item.title">
                <span class="file-dot"></span>
                <div><strong>{{ item.title }}</strong><span>{{ item.type }}</span></div>
                <time>{{ item.time }}</time>
              </li>
            </ul>
          </article>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.workspace-page {
  min-height: 100vh;
  padding: clamp(18px, 3vw, 44px);
  color: #17181c;
  background: #d8d5cf;
  --el-color-primary: #17181c;
  --el-color-primary-light-3: #3b3c42;
  --el-color-primary-light-5: #66676c;
  --el-color-primary-light-7: #a7a7aa;
  --el-color-primary-light-9: #eeeeec;
  --el-border-radius-base: 14px;
}

.workspace-shell {
  display: grid;
  width: min(1480px, 100%);
  min-height: calc(100vh - clamp(36px, 6vw, 88px));
  margin: 0 auto;
  overflow: hidden;
  grid-template-columns: 238px minmax(0, 1fr);
  border-radius: 34px;
  background: #f5f2ec;
  box-shadow: 0 30px 70px rgb(35 32 27 / 16%);
}

.workspace-sidebar {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 30px 22px 24px;
  border-right: 1px solid #e1ddd5;
  background: #fbfaf7;
}

.workspace-brand { display: flex; align-items: center; gap: 11px; padding: 0 8px; font-weight: 800; letter-spacing: -0.03em; }
.workspace-logo { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 14px; background: #ffd84d; font-weight: 900; }
.workspace-sidebar nav { display: grid; gap: 8px; margin-top: 54px; }
.workspace-nav { display: flex; min-height: 48px; align-items: center; gap: 12px; padding: 0 16px; border-radius: 18px; color: #66645f; font-size: 14px; text-decoration: none; }
.nav-button { width: 100%; border: 0; background: transparent; cursor: pointer; }
.workspace-nav.active { color: #17181c; background: #e7e3dc; font-weight: 750; }
.sidebar-spacer { flex: 1; }
.workspace-profile { display: flex; align-items: center; gap: 10px; }
.workspace-profile :deep(.el-avatar) { color: #fff; background: #f05a18; }
.workspace-profile div { min-width: 0; }
.workspace-profile strong, .workspace-profile span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.workspace-profile span { margin-top: 3px; color: #88857e; font-size: 11px; }

.workspace-main { min-width: 0; padding: 38px clamp(24px, 4vw, 54px) 44px; }
.workspace-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; }
.workspace-header p { margin: 0 0 10px; color: #f05a18; font-size: 11px; font-weight: 800; letter-spacing: .15em; }
.workspace-header h1 { margin: 0; font-size: clamp(30px, 3.4vw, 48px); letter-spacing: -.055em; }
.workspace-header span { display: block; margin-top: 10px; color: #77746e; line-height: 1.6; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.header-actions :deep(.el-button) { min-height: 48px; padding-inline: 18px; }
.search-input { width: min(280px, 25vw); }
.search-input :deep(.el-input__wrapper) { min-height: 48px; padding: 0 18px; border-radius: 24px; box-shadow: none; background: #fff; }

.dashboard-grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 18px; margin-top: 38px; }
.dashboard-grid article { overflow: hidden; }
.agent-panel, .feature-card, .recent-card { background: #fff; }
.agent-panel { grid-column: span 8; padding: clamp(24px, 3vw, 38px); border-radius: 30px; }
.progress-panel { grid-column: span 4; padding: 28px; border-radius: 30px; color: #fff; background: #24262d; }
.panel-heading, .dark-heading, .recent-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.panel-heading h2 { margin: 16px 0 24px; font-size: clamp(24px, 2.6vw, 36px); letter-spacing: -.04em; }
.panel-number { color: #bbb5aa; font-size: 42px; font-weight: 850; }
.agent-panel :deep(.el-textarea__inner) { padding: 18px; border: 0; border-radius: 20px; box-shadow: none; background: #f1eee8; line-height: 1.7; }
.composer-actions { display: flex; align-items: center; gap: 12px; margin-top: 18px; }
.composer-actions > span { flex: 1; color: #908c84; font-size: 11px; }
.composer-actions :deep(.el-button) { min-height: 44px; padding-inline: 20px; }
.send-button { min-width: 108px; }

.dark-heading span { display: block; color: #b9bac0; font-size: 13px; }
.dark-heading strong { display: block; margin-top: 5px; font-size: 34px; }
.dark-heading :deep(.el-tag) { border: 0; color: #17181c; }
.progress-ring { display: grid; width: 126px; height: 126px; margin: 26px auto; place-content: center; border: 13px solid #34363e; border-top-color: #ffd84d; border-right-color: #f05a18; border-radius: 50%; text-align: center; }
.progress-ring span { font-size: 32px; font-weight: 850; }
.progress-ring small { color: #b9bac0; }
.progress-panel ul, .recent-card ul { margin: 0; padding: 0; list-style: none; }
.progress-panel li { display: flex; justify-content: space-between; padding: 10px 0; border-top: 1px solid #3b3d45; color: #c8c9cc; font-size: 12px; }
.progress-panel li strong { color: #fff; }

.feature-card { grid-column: span 4; min-height: 260px; padding: 28px; border-radius: 26px; }
.feature-icon { display: grid; width: 48px; height: 48px; place-items: center; border-radius: 17px; font-size: 22px; }
.feature-icon.orange { color: #fff; background: #f05a18; }
.feature-icon.yellow { color: #17181c; background: #ffd84d; }
.card-index { display: block; margin-top: 22px; color: #8a867e; font-size: 10px; font-weight: 800; letter-spacing: .1em; }
.feature-card h3 { margin: 8px 0 0; font-size: 24px; }
.feature-card p { min-height: 66px; margin: 12px 0 18px; color: #74716a; font-size: 13px; line-height: 1.7; }
.feature-card :deep(.el-button) { min-height: 42px; }
.feature-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.feature-actions :deep(.el-button) { margin: 0; }

.recent-card { grid-column: span 4; padding: 26px; border-radius: 26px; }
.recent-heading span { color: #f05a18; font-size: 10px; font-weight: 800; letter-spacing: .12em; }
.recent-heading h3 { margin: 6px 0 0; font-size: 24px; }
.recent-card li { display: grid; min-height: 58px; align-items: center; grid-template-columns: 10px minmax(0, 1fr) auto; gap: 11px; border-top: 1px solid #eeece7; }
.file-dot { width: 7px; height: 7px; border-radius: 50%; background: #f05a18; }
.recent-card strong, .recent-card li span { display: block; }
.recent-card li span, .recent-card time { margin-top: 3px; color: #8d8981; font-size: 10px; }

@media (max-width: 1050px) {
  .workspace-shell { grid-template-columns: 92px minmax(0, 1fr); }
  .workspace-sidebar { align-items: center; padding-inline: 16px; }
  .workspace-brand > span:last-child, .workspace-nav span, .workspace-profile div { display: none; }
  .workspace-sidebar nav { width: 100%; }
  .workspace-nav { justify-content: center; padding: 0; font-size: 18px; }
  .agent-panel, .progress-panel { grid-column: span 12; }
  .feature-card, .recent-card { grid-column: span 6; }
  .header-actions { align-items: flex-end; flex-direction: column; }
  .search-input { width: min(280px, 34vw); }
}

@media (max-width: 720px) {
  .workspace-page { padding: 0; }
  .workspace-shell { display: block; min-height: 100vh; border-radius: 0; }
  .workspace-sidebar { display: none; }
  .workspace-main { padding: 24px 16px 32px; }
  .workspace-header { display: block; }
  .header-actions { display: grid; align-items: stretch; margin-top: 20px; }
  .search-input { width: 100%; }
  .dashboard-grid { margin-top: 24px; }
  .feature-card, .recent-card { grid-column: span 12; }
  .composer-actions { align-items: stretch; flex-direction: column; }
  .composer-actions > span { order: 3; }
  .composer-actions :deep(.el-button) { width: 100%; margin: 0; }
}
</style>
