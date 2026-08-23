<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <div class="auth-shell">
    <section class="auth-story" aria-label="Personal Agent 产品介绍">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">P</span>
        <span>Personal Agent</span>
      </div>
      <div class="story-copy">
        <p class="eyebrow">你的个人 Agent 工作台</p>
        <h1>把学习与工作，整理成可复用的成果。</h1>
        <p class="story-description">
          从课程录音到周报素材，让零散信息在一个安静、清晰的空间里持续沉淀。
        </p>
        <ul class="feature-list">
          <li>课程录音与结构化笔记</li>
          <li>对话式周报收集与生成</li>
          <li>可替换的 AI Provider 边界</li>
        </ul>
      </div>
      <p class="build-note">当前阶段 · 多用户认证与个人工作台</p>
    </section>

    <main class="auth-main">
      <section class="auth-card">
        <header class="auth-card-header">
          <p class="mobile-brand">Personal Agent</p>
          <h2>{{ title }}</h2>
          <p>{{ description }}</p>
        </header>
        <slot />
      </section>
      <p class="auth-footer">Personal Agent · Java 21 + Spring Boot + Vue 3</p>
    </main>
  </div>
</template>

<style scoped>
.auth-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(360px, 0.9fr) minmax(480px, 1.1fr);
}

.auth-story {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 5vw, 72px);
  color: #ecfdf5;
  background: #115e59;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 45%);
  border-radius: 10px;
  color: #134e4a;
  background: #ccfbf1;
}

.story-copy {
  max-width: 560px;
  padding-block: 56px;
}

.eyebrow {
  margin: 0 0 16px;
  color: #99f6e4;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h1 {
  max-width: 12em;
  margin: 0;
  font-size: clamp(38px, 4.4vw, 64px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

.story-description {
  max-width: 34em;
  margin: 28px 0 0;
  color: #ccfbf1;
  font-size: 17px;
  line-height: 1.8;
}

.feature-list {
  display: grid;
  gap: 14px;
  margin: 36px 0 0;
  padding: 0;
  list-style: none;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
}

.feature-list li::before {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: #5eead4;
  content: '';
}

.build-note {
  margin: 0;
  color: #99f6e4;
  font-size: 13px;
}

.auth-main {
  display: grid;
  min-height: 100%;
  place-items: center;
  align-content: center;
  padding: 40px clamp(24px, 7vw, 96px);
  background: var(--color-background);
}

.auth-card {
  width: min(100%, 440px);
  padding: 40px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
}

.auth-card-header h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: -0.025em;
}

.auth-card-header > p:not(.mobile-brand) {
  margin: 12px 0 28px;
  color: var(--color-muted);
  line-height: 1.65;
}

.mobile-brand {
  display: none;
  margin: 0 0 28px;
  color: var(--color-primary);
  font-weight: 700;
}

.auth-footer {
  margin: 28px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 860px) {
  .auth-shell {
    display: block;
  }

  .auth-story {
    display: none;
  }

  .auth-main {
    min-height: 100vh;
    padding: 24px;
  }

  .mobile-brand {
    display: block;
  }
}

@media (max-width: 480px) {
  .auth-main {
    padding: 16px;
  }

  .auth-card {
    padding: 28px 22px;
    border-radius: 16px;
  }
}
</style>
