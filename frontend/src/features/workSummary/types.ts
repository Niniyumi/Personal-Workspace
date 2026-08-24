export type SummaryPeriodType = 'QUARTER' | 'YEAR'

export interface WorkSummarySelection {
  periodType: SummaryPeriodType
  year: number
  quarter?: number
}

export interface WorkSummaryInput {
  coreContent: string
  routineWork: string
  selfScore: number
}

export interface WorkSummary extends WorkSummaryInput {
  id: number
  periodType: SummaryPeriodType
  periodStart: string
  periodEnd: string
  generatedAt: string
  createdAt: string
  updatedAt: string
}
