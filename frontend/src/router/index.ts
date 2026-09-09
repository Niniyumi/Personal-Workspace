import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore'

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes: [
      {
        path: '/',
        name: 'home',
        component: () => import('../views/HomeView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/login',
        name: 'login',
        component: () => import('../views/LoginView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: '/register',
        name: 'register',
        component: () => import('../views/RegisterView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: '/forgot-password',
        name: 'forgot-password',
        component: () => import('../views/ForgotPasswordView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: '/weekly-reports',
        name: 'weekly-reports',
        component: () => import('../views/WeeklyReportsView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/weekly-reports/:id',
        name: 'weekly-report-detail',
        component: () => import('../views/WeeklyReportDetailView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/work-summaries',
        name: 'work-summaries',
        component: () => import('../views/WorkSummariesView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/courses',
        name: 'courses',
        component: () => import('../views/CoursesView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/courses/:id',
        name: 'course-detail',
        component: () => import('../views/CourseDetailView.vue'),
        meta: { requiresAuth: true },
      },
      { path: '/:pathMatch(.*)*', redirect: '/' },
    ],
  })

  router.beforeEach(async (to) => {
    const store = useAuthStore()
    if (store.status === 'idle') {
      // 首次打开网页时先恢复本地会话，避免已登录用户看到登录页闪烁。
      await store.restoreSession()
    }
    if (to.meta.requiresAuth && store.status !== 'authenticated') {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (to.meta.guestOnly && store.status === 'authenticated') {
      return { name: 'home' }
    }
  })

  return router
}

export default createAppRouter()
