export type StudyTaskType = 'WRONG_REVIEW' | 'WEAK_POINT' | 'NEW_KNOWLEDGE' | 'REAL_EXAM'
export type AnswerConfidence = 'GUESS' | 'UNCERTAIN' | 'CONFIDENT'
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'CASE' | 'ESSAY'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type FoundationLevel = 'ZERO' | 'SOME' | 'REVIEWING'

export interface DailyTaskDto {
  taskId: number
  taskType: StudyTaskType
  knowledgeId: number | null
  estimatedMinutes: number
  priorityScore: number | null
}

export interface DailyPlanDto {
  planId: number
  planDate: string
  targetMinutes: number
  tasks: DailyTaskDto[]
}

export interface AssessmentQuestionDto {
  id: number
  type: QuestionType
  difficulty: Difficulty
  content: string
  options?: QuestionOptionDto[]
}

export interface QuestionOptionDto {
  key: string
  text: string
}

export interface ExamDto {
  id: number
  code: string
  name: string
}

export interface WechatLoginResponse {
  token: string
  userId: number
  profileCompleted?: boolean
}

export interface StartAssessmentResponse {
  sessionId: string
}

export interface AssessmentAnswerResponse {
  questionId: number
  correct: boolean
  answerRecordId: string
}

export interface AssessmentSubmitResponse {
  totalQuestions: number
  correctQuestions: number
}


export interface CourseSummaryDto {
  id: number
  examId: number
  title: string
  description?: string | null
}

export interface CourseChapterDto {
  id: number
  courseId: number
  title: string
  description?: string | null
  videos: VideoSummaryDto[]
}

export interface CourseDetailDto extends CourseSummaryDto {
  chapters: CourseChapterDto[]
}

export interface VideoSummaryDto {
  id: number
  chapterId: number
  title: string
  description?: string | null
  durationSeconds: number
  freeFlag: boolean
}

export interface VideoTranscriptSegmentDto {
  startSeconds: number
  endSeconds: number
  text: string
}

export interface VideoDetailDto extends VideoSummaryDto {
  playUrl?: string | null
  progressSeconds?: number
  completed?: boolean
  transcript?: VideoTranscriptSegmentDto[]
  knowledgeIds?: number[]
}

export interface KnowledgeDetailDto {
  id: number
  name: string
  description?: string | null
  masteryScore?: number | null
  evidenceCount: number
  videos?: VideoSummaryDto[]
}


export type PracticeSource = 'CHAPTER' | 'REAL_EXAM' | 'WRONG_REVIEW' | 'AI_QUIZ'

export interface PracticeQuestionDto extends AssessmentQuestionDto {
  source?: string
  options?: QuestionOptionDto[]
}

export interface QuestionSessionStartResponse {
  sessionId: string
}

export interface QuestionAnswerFeedbackDto {
  questionId: number
  correct: boolean
  answerRecordId?: string
  standardAnswer?: string | null
  explanation?: string | null
}

export interface WrongQuestionDto {
  id: number
  questionId: number
  status: 'ACTIVE' | 'MASTERED'
  wrongCount: number
  consecutiveCorrect?: number
  lastWrongAt?: string
  question?: PracticeQuestionDto
}

export interface LocalFavoriteQuestion {
  question: PracticeQuestionDto
  savedAt: string
}


export interface AiChatResponse {
  content: string
  requestId?: string
}

export interface WeeklyKnowledgeStatDto {
  knowledgeId: number
  name: string
  masteryScore?: number | null
  evidenceCount: number
}

export interface WeeklyStatsDto {
  weekStart: string
  weekEnd: string
  studyMinutes: number
  completedTasks: number
  answeredQuestions: number
  correctQuestions: number
  weakKnowledge: WeeklyKnowledgeStatDto[]
}
