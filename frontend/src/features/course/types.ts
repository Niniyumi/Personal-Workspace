export type CourseStatus = 'UPLOADING' | 'RECORDING' | 'PROCESSING' | 'TRANSCRIBED' | 'READY' | 'FAILED'

export interface Course {
  id: number
  title: string
  courseName?: string
  lessonDate?: string
  status: CourseStatus
  durationSeconds: number
  processingProgress: number
  transcript: string | null
  noteContent: string | null
  noteCandidate: string | null
  errorMessage: string | null
  sourceType?: 'IMPORT' | 'RECORDING'
  createdAt: string
  updatedAt: string
}

export type CourseSummary = Pick<Course,
  'id' | 'title' | 'courseName' | 'lessonDate' | 'status' | 'durationSeconds' | 'processingProgress' | 'errorMessage' | 'createdAt' | 'updatedAt'>

export interface CourseAudioPart {
  id: number
  partNumber: number
  durationSeconds: number
  fileSize: number
}

export type CourseProgress = Pick<Course, 'status' | 'processingProgress' | 'errorMessage'>
