import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import RegisterView from '../views/RegisterView.vue'
import WeeklyReportDetailView from '../views/WeeklyReportDetailView.vue'
import WeeklyReportsView from '../views/WeeklyReportsView.vue'
import WorkSummariesView from '../views/WorkSummariesView.vue'
import CoursesView from '../views/CoursesView.vue'
import CourseDetailView from '../views/CourseDetailView.vue'

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes: [
      {
        path: '/',
        name: 'home',
        component: HomeView,
        meta: { requiresAuth: true },
      },
      {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: { guestOnly: true },
      },
      {
        path: '/register',
        name: 'register',
        component: RegisterView,
        meta: { guestOnly: true },
      },
      {
        path: '/forgot-password',
        name: 'forgot-password',
        component: ForgotPasswordView,
        meta: { guestOnly: true },
      },
      {
        path: '/weekly-reports',
        name: 'weekly-reports',
        component: WeeklyReportsView,
        meta: { requiresAuth: true },
      },
      {
        path: '/weekly-reports/:id',
        name: 'weekly-report-detail',
        component: WeeklyReportDetailView,
        meta: { requiresAuth: true },
      },
      {
        path: '/work-summaries',
        name: 'work-summaries',
        component: WorkSummariesView,
        meta: { requiresAuth: true },
      },
      {
        path: '/courses',
        name: 'courses',
        component: CoursesView,
        meta: { requiresAuth: true },
      },
      {
        path: '/courses/:id',
        name: 'course-detail',
        component: CourseDetailView,
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
