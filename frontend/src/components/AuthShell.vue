<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-story" aria-label="Personal Agent 产品介绍">
        <div class="brand">
          <span class="brand-mark" aria-hidden="true">P</span>
          <span>Personal Agent</span>
        </div>
        <div class="story-copy">
          <p class="eyebrow">PERSONAL WORKSPACE</p>
          <h1>把零散的日常，整理成自己的成果。</h1>
          <p class="story-description">
            从课程录音到周报素材，让学习和工作在一个柔和、清晰的空间里持续沉淀。
          </p>
          <ul class="feature-list">
            <li>课程录音与结构化笔记</li>
            <li>对话式周报收集与生成</li>
            <li>灵活切换 AI Provider</li>
          </ul>
        </div>
        <div class="story-decoration" aria-hidden="true">
          <span></span><span></span><span></span>
        </div>
        <p class="build-note">YOUR OWN AGENT · ISSUE 01</p>
      </section>

      <main class="auth-main">
        <section class="auth-card">
          <header class="auth-card-header">
            <p class="mobile-brand">Personal Agent</p>
            <p class="form-index">ACCOUNT / 01</p>
            <h2>{{ title }}</h2>
            <p>{{ description }}</p>
          </header>
          <slot />
        </section>
        <p class="auth-footer">Personal Agent · Java 21 + Spring Boot + Vue 3</p>
      </main>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: clamp(18px, 3vw, 44px);
  color: #17181c;
  background: #d8d5cf;
}

.auth-shell {
  display: grid;
  width: min(1320px, 100%);
  min-height: calc(100vh - clamp(36px, 6vw, 88px));
  margin: 0 auto;
  overflow: hidden;
  grid-template-columns: minmax(460px, 1.08fr) minmax(440px, 0.92fr);
  border-radius: 34px;
  background: #f5f2ec;
  box-shadow: 0 30px 70px rgb(35 32 27 / 16%);
  --color-primary: #17181c;
  --color-primary-hover: #34353a;
  --color-text: #17181c;
  --color-muted: #77746e;
  --color-border: #ded9d0;
  --color-focus: #f05a18;
}

.auth-story {
  position: relative;
  display: flex;
  min-height: 100%;
  overflow: hidden;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 5vw, 72px);
  color: #17181c;
  background: #cec6b7;
}

.brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 15px;
  background: #ffd84d;
  font-weight: 900;
}

.story-copy {
  position: relative;
  z-index: 1;
  max-width: 560px;
  padding-block: 56px;
}

.eyebrow {
  margin: 0 0 16px;
  color: #f05a18;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  max-width: 10em;
  margin: 0;
  font-size: clamp(42px, 4.6vw, 68px);
  line-height: 1.08;
  letter-spacing: -0.06em;
}

.story-description {
  max-width: 34em;
  margin: 28px 0 0;
  color: #504d47;
  font-size: 16px;
  line-height: 1.85;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 36px 0 0;
  padding: 0;
  list-style: none;
}

.feature-list li {
  display: flex;
  min-height: 38px;
  align-items: center;
  gap: 12px;
  padding: 0 15px;
  border: 1px solid rgb(23 24 28 / 12%);
  border-radius: 999px;
  background: rgb(255 255 255 / 30%);
  font-size: 13px;
}

.feature-list li::before {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f05a18;
  content: '';
}

.build-note {
  position: relative;
  z-index: 1;
  margin: 0;
  color: #625e56;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.story-decoration {
  position: absolute;
  right: -90px;
  bottom: 72px;
  width: 330px;
  height: 280px;
}

.story-decoration span {
  position: absolute;
  display: block;
  border-radius: 50%;
  opacity: 0.92;
}

.story-decoration span:nth-child(1) { width: 230px; height: 230px; right: 0; bottom: 0; background: #ffd84d; }
.story-decoration span:nth-child(2) { width: 150px; height: 150px; left: 0; bottom: 5px; background: #f05a18; }
.story-decoration span:nth-child(3) { width: 92px; height: 92px; left: 94px; top: 0; background: #24262d; }

.auth-main {
  display: grid;
  min-height: 100%;
  place-items: center;
  align-content: center;
  padding: 40px clamp(28px, 5vw, 76px);
  background: #f5f2ec;
}

.auth-card {
  width: min(100%, 470px);
  padding: clamp(30px, 4vw, 48px);
  border-radius: 30px;
  background: #fff;
  box-shadow: 0 18px 45px rgb(35 32 27 / 9%);
}

.auth-card-header h2 {
  margin: 0;
  color: #17181c;
  font-size: clamp(32px, 3vw, 42px);
  line-height: 1.25;
  letter-spacing: -0.05em;
}

.auth-card-header > p:not(.mobile-brand) {
  margin: 12px 0 28px;
  color: var(--color-muted);
  line-height: 1.65;
}

.auth-card-header > .form-index {
  margin: 0 0 12px;
  color: #f05a18;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.mobile-brand {
  display: none;
  margin: 0 0 28px;
  color: #17181c;
  font-weight: 800;
}

.auth-footer {
  margin: 28px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 860px) {
  .auth-page { padding: 0; }

  .auth-shell {
    display: block;
    min-height: 100vh;
    border-radius: 0;
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
    border-radius: 26px;
  }
}
</style>
