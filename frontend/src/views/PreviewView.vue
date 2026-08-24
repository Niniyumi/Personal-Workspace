<script setup lang="ts">
import {
  ChatDotRound,
  Document,
  FolderOpened,
  HomeFilled,
  Mic,
  Promotion,
  Search,
  Setting,
  Upload,
} from '@element-plus/icons-vue'
import { ElAvatar, ElButton, ElIcon, ElInput, ElTag } from 'element-plus'

const recentItems = [
  { title: '第 3 周工作素材', type: '周报草稿', time: '今天 14:20' },
  { title: '分布式系统课程', type: '课程录音', time: '昨天 21:08' },
  { title: 'Spring Security 总结', type: '个人笔记', time: '8 月 22 日' },
]
</script>

<template>
  <div class="preview-page">
    <div class="preview-shell">
      <aside class="preview-sidebar">
        <div class="preview-brand"><span class="preview-logo">P</span><span>Personal Agent</span></div>

        <nav aria-label="静态预览导航">
          <a class="preview-nav active" href="#workspace"><ElIcon><HomeFilled /></ElIcon><span>工作台</span></a>
          <a class="preview-nav" href="#weekly"><ElIcon><ChatDotRound /></ElIcon><span>周报助手</span></a>
          <a class="preview-nav" href="#course"><ElIcon><Mic /></ElIcon><span>课程笔记</span></a>
          <a class="preview-nav" href="#archive"><ElIcon><FolderOpened /></ElIcon><span>资料归档</span></a>
        </nav>

        <div class="sidebar-spacer"></div>
        <button class="icon-nav" type="button" aria-label="设置"><ElIcon><Setting /></ElIcon></button>
        <div class="preview-profile">
          <ElAvatar :size="42">N</ElAvatar>
          <div><strong>Nini</strong><span>静态预览模式</span></div>
        </div>
      </aside>

      <main id="workspace" class="preview-main">
        <header class="preview-header">
          <div>
            <p>PERSONAL WORKSPACE</p>
            <h1>今天想整理些什么？</h1>
            <span>把零散的文字、录音和文档交给你的个人 Agent。</span>
          </div>
          <ElInput class="search-input" placeholder="搜索你的资料" :prefix-icon="Search" />
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
              placeholder="例如：这周完成了登录模块，补充了接口测试，下一步准备做周报对话……"
            />
            <div class="composer-actions">
              <ElButton round :icon="Upload">上传文档</ElButton>
              <span>支持文字与 DOCX，当前页面不发送真实数据</span>
              <ElButton class="send-button" round type="primary" :icon="Promotion">发送</ElButton>
            </div>
          </article>

          <article class="progress-panel">
            <div class="dark-heading">
              <div><span>本周整理进度</span><strong>68%</strong></div>
              <ElTag round color="#ffd84d">WEEK 34</ElTag>
            </div>
            <div class="progress-ring"><span>6</span><small>项成果</small></div>
            <ul>
              <li><span>周报素材</span><strong>4 / 5</strong></li>
              <li><span>课程录音</span><strong>2 / 3</strong></li>
              <li><span>归档文档</span><strong>8 / 12</strong></li>
            </ul>
          </article>

          <article id="weekly" class="feature-card">
            <div class="feature-icon orange"><ElIcon><Document /></ElIcon></div>
            <span class="card-index">02 / WEEKLY REPORT</span>
            <h3>对话式周报</h3>
            <p>持续添加每天完成的工作，准备好后再由 Agent 汇总成一份可编辑周报。</p>
            <ElButton round>继续整理</ElButton>
          </article>

          <article id="course" class="feature-card">
            <div class="feature-icon yellow"><ElIcon><Mic /></ElIcon></div>
            <span class="card-index">03 / COURSE NOTES</span>
            <h3>课程笔记</h3>
            <p>录音完成后自动转写，提炼关键概念、待办事项和复习提纲。</p>
            <ElButton round>新建课程</ElButton>
          </article>

          <article id="archive" class="recent-card">
            <div class="recent-heading">
              <div><span>RECENT FILES</span><h3>最近整理</h3></div>
              <ElButton text>查看全部</ElButton>
            </div>
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
.preview-page {
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

.preview-shell {
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

.preview-sidebar {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 30px 22px 24px;
  border-right: 1px solid #e1ddd5;
  background: #fbfaf7;
}

.preview-brand { display: flex; align-items: center; gap: 11px; padding: 0 8px; font-weight: 800; letter-spacing: -0.03em; }
.preview-logo { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 14px; background: #ffd84d; font-weight: 900; }
.preview-sidebar nav { display: grid; gap: 8px; margin-top: 54px; }
.preview-nav { display: flex; min-height: 48px; align-items: center; gap: 12px; padding: 0 16px; border-radius: 18px; color: #66645f; font-size: 14px; text-decoration: none; }
.preview-nav.active { color: #17181c; background: #e7e3dc; font-weight: 750; }
.sidebar-spacer { flex: 1; }
.icon-nav { display: grid; width: 46px; height: 46px; margin-bottom: 18px; place-items: center; border: 0; border-radius: 50%; color: #17181c; background: #efede8; cursor: pointer; }
.preview-profile { display: flex; align-items: center; gap: 10px; }
.preview-profile :deep(.el-avatar) { color: #fff; background: #f05a18; }
.preview-profile strong, .preview-profile span { display: block; }
.preview-profile span { margin-top: 3px; color: #88857e; font-size: 11px; }

.preview-main { min-width: 0; padding: 38px clamp(24px, 4vw, 54px) 44px; }
.preview-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; }
.preview-header p { margin: 0 0 10px; color: #f05a18; font-size: 11px; font-weight: 800; letter-spacing: .15em; }
.preview-header h1 { margin: 0; font-size: clamp(30px, 3.4vw, 48px); letter-spacing: -.055em; }
.preview-header span { display: block; margin-top: 10px; color: #77746e; line-height: 1.6; }
.search-input { width: min(310px, 28vw); }
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

.recent-card { grid-column: span 4; padding: 26px; border-radius: 26px; }
.recent-heading span { color: #f05a18; font-size: 10px; font-weight: 800; letter-spacing: .12em; }
.recent-heading h3 { margin: 6px 0 0; font-size: 24px; }
.recent-card li { display: grid; min-height: 58px; align-items: center; grid-template-columns: 10px minmax(0, 1fr) auto; gap: 11px; border-top: 1px solid #eeece7; }
.file-dot { width: 7px; height: 7px; border-radius: 50%; background: #f05a18; }
.recent-card strong, .recent-card li span { display: block; }
.recent-card li span, .recent-card time { margin-top: 3px; color: #8d8981; font-size: 10px; }

@media (max-width: 1050px) {
  .preview-shell { grid-template-columns: 92px minmax(0, 1fr); }
  .preview-sidebar { align-items: center; padding-inline: 16px; }
  .preview-brand > span:last-child, .preview-nav span, .preview-profile div { display: none; }
  .preview-sidebar nav { width: 100%; }
  .preview-nav { justify-content: center; padding: 0; font-size: 18px; }
  .agent-panel, .progress-panel { grid-column: span 12; }
  .feature-card, .recent-card { grid-column: span 6; }
}

@media (max-width: 720px) {
  .preview-page { padding: 0; }
  .preview-shell { display: block; min-height: 100vh; border-radius: 0; }
  .preview-sidebar { display: none; }
  .preview-main { padding: 24px 16px 32px; }
  .preview-header { display: block; }
  .search-input { width: 100%; margin-top: 20px; }
  .dashboard-grid { margin-top: 24px; }
  .feature-card, .recent-card { grid-column: span 12; }
  .composer-actions { align-items: stretch; flex-direction: column; }
  .composer-actions > span { order: 3; }
  .composer-actions :deep(.el-button) { width: 100%; margin: 0; }
}
</style>
