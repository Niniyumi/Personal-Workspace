export type CourseStatus = 'RECORDING' | 'PROCESSING' | 'READY' | 'FAILED'

export interface Course {
  id: number
  title: string
  status: CourseStatus
  durationSeconds: number
  transcript: string | null
  noteContent: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export type CourseSummary = Pick<Course,
  'id' | 'title' | 'status' | 'durationSeconds' | 'errorMessage' | 'createdAt' | 'updatedAt'>

export interface CourseAudioPart {
  id: number
  partNumber: number
  durationSeconds: number
  fileSize: number
}
