<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore'

const store = useAuthStore()
const router = useRouter()

async function logout() {
  await store.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="workspace-shell">
    <aside class="workspace-sidebar">
      <div class="brand brand-dark">
        <span class="brand-mark" aria-hidden="true">P</span>
        <span>Personal Agent</span>
      </div>
      <nav aria-label="主要导航">
        <a class="nav-item active" href="#agent">Agent 首页</a>
        <span class="nav-item muted">课程笔记 <small>下一阶段</small></span>
        <span class="nav-item muted">周报助手 <small>下一阶段</small></span>
      </nav>
      <div class="account-card">
        <span class="avatar" aria-hidden="true">{{ store.user?.displayName.slice(0, 1) }}</span>
        <div>
          <strong>{{ store.user?.displayName }}</strong>
          <span>{{ store.user?.email }}</span>
        </div>
      </div>
    </aside>

    <main id="agent" class="workspace-main">
      <header class="workspace-header">
        <div>
          <p class="eyebrow-dark">个人工作台</p>
          <h1>晚上好，{{ store.user?.displayName }}</h1>
          <p>认证系统已经连接，接下来可以从周报或课程工具继续搭建。</p>
        </div>
        <button data-test="logout" class="secondary-button" type="button" @click="logout">退出登录</button>
      </header>

      <section class="agent-card" aria-labelledby="agent-title">
        <div class="agent-status"><span aria-hidden="true"></span> Agent 已就绪</div>
        <h2 id="agent-title">今天想整理什么？</h2>
        <p>登录功能已经可用。周报对话和课程录音会在后续增量接入这里。</p>
        <div class="prompt-box" aria-disabled="true">
          <span>告诉 Agent 你想记录的内容…</span>
          <button type="button" disabled>发送</button>
        </div>
      </section>

      <section class="workspace-grid" aria-label="功能进度">
        <article>
          <span class="step-number">01</span>
          <h3>账号与安全</h3>
          <p>注册、登录、刷新令牌和退出流程已经完成。</p>
          <strong class="status-ready">已完成</strong>
        </article>
        <article>
          <span class="step-number">02</span>
          <h3>对话式周报</h3>
          <p>持续添加文字和 DOCX，再由你决定何时生成周报。</p>
          <strong>准备开发</strong>
        </article>
        <article>
          <span class="step-number">03</span>
          <h3>课程笔记</h3>
          <p>浏览器长录音、语音识别和结构化课程笔记。</p>
          <strong>后续阶段</strong>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 264px minmax(0, 1fr);
  background: #f8fafc;
}

.workspace-sidebar {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  padding: 28px 22px;
  border-right: 1px solid #dbe7e5;
  background: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 750;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 9px;
  color: #ecfdf5;
  background: var(--color-primary);
}

nav {
  display: grid;
  gap: 8px;
  margin-top: 48px;
}

.nav-item {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-radius: 10px;
  color: #334155;
  font-size: 14px;
  text-decoration: none;
}

.nav-item.active {
  color: #115e59;
  background: #ccfbf1;
  font-weight: 700;
}

.nav-item.muted {
  color: #64748b;
}

.nav-item small {
  font-size: 10px;
}

.account-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 22px;
  border-top: 1px solid #e2e8f0;
}

.avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  color: #115e59;
  background: #ccfbf1;
  font-weight: 700;
}

.account-card div {
  min-width: 0;
}

.account-card strong,
.account-card span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card span {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
}

.workspace-main {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 48px clamp(24px, 5vw, 72px);
}

.workspace-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.eyebrow-dark {
  margin: 0 0 8px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.workspace-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(30px, 4vw, 46px);
  letter-spacing: -0.04em;
}

.workspace-header p:last-child {
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.secondary-button {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  color: #334155;
  background: #fff;
  cursor: pointer;
}

.agent-card {
  margin-top: 44px;
  padding: clamp(28px, 5vw, 56px);
  border: 1px solid #99f6e4;
  border-radius: 20px;
  background: #f0fdfa;
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.agent-status span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #14b8a6;
}

.agent-card h2 {
  margin: 22px 0 0;
  color: #134e4a;
  font-size: clamp(26px, 3vw, 38px);
}

.agent-card > p {
  margin: 12px 0 0;
  color: #475569;
  line-height: 1.7;
}

.prompt-box {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 30px;
  padding: 8px 8px 8px 18px;
  border: 1px solid #99f6e4;
  border-radius: 12px;
  color: #64748b;
  background: #fff;
}

.prompt-box button {
  min-width: 74px;
  min-height: 42px;
  border: 0;
  border-radius: 9px;
  color: #94a3b8;
  background: #e2e8f0;
}

.workspace-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 22px;
}

.workspace-grid article {
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
}

.step-number {
  color: #0d9488;
  font-size: 12px;
  font-weight: 800;
}

.workspace-grid h3 {
  margin: 18px 0 0;
  color: #0f172a;
}

.workspace-grid p {
  min-height: 4.8em;
  margin: 10px 0 20px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.workspace-grid strong {
  color: #64748b;
  font-size: 12px;
}

.workspace-grid .status-ready {
  color: #0f766e;
}

@media (max-width: 900px) {
  .workspace-shell {
    display: block;
  }

  .workspace-sidebar {
    min-height: auto;
    padding: 18px 22px;
    border-right: 0;
    border-bottom: 1px solid #dbe7e5;
  }

  .workspace-sidebar nav,
  .account-card {
    display: none;
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .workspace-grid p {
    min-height: auto;
  }
}

@media (max-width: 560px) {
  .workspace-main {
    padding: 30px 18px;
  }

  .workspace-header {
    display: grid;
  }

  .secondary-button {
    width: 100%;
  }

  .prompt-box {
    align-items: flex-start;
    flex-direction: column;
  }

  .prompt-box button {
    width: 100%;
  }
}
</style>
