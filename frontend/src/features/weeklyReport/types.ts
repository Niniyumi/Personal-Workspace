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

export interface WeeklyReportInput {
  weekStartDate: string
  coreWork: string
  problems: string
  nextWeekPlan: string
  sourceFileName: string | null
}

export interface DocxImportResult {
  coreWork: string
  problems: string | null
  nextWeekPlan: string | null
  sourceFileName: string
}
