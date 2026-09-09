export interface MonthlyActivity {
  month: string
  weeklyReports: number
  courseNotes: number
}

export interface DashboardRecentItem {
  type: 'WEEKLY_REPORT' | 'COURSE'
  id: number
  title: string
  updatedAt: string
}

export interface DashboardData {
  totalWeeklyReports: number
  totalCourseNotes: number
  totalRecordingSeconds: number
  monthWeeklyReports: number
  monthCourseNotes: number
  monthRecordingSeconds: number
  monthlyActivity: MonthlyActivity[]
  recentItems: DashboardRecentItem[]
}
