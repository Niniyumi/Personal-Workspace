export interface WeeklyReport {
  id: number
  weekStartDate: string
  coreWork: string
  problems: string | null
  nextWeekPlan: string | null
  sourceFileName: string | null
  createdAt: string
  updatedAt: string
}

export interface WeeklyReportListItem {
  id: number
  weekStartDate: string
  createdAt: string
  sourceFileName: string | null
  characterCount: number
  preview: string
}

export interface WeeklyReportSearchResult {
  items: WeeklyReportListItem[]
  total: number
  page: number
}

export interface WeeklyReportInput {
  weekStartDate: string
  coreWork: string
  problems: string
  nextWeekPlan: string
  sourceFileName: string | null
}

export interface DocxImportResult {
  weekStartDate: string | null
  coreWork: string
  problems: string | null
  nextWeekPlan: string | null
  sourceFileName: string
}
