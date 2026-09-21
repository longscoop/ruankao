export type StudyTaskType = 'WRONG_REVIEW' | 'WEAK_KNOWLEDGE' | 'NEW_KNOWLEDGE' | 'REAL_EXAM'
export type AnswerConfidence = 'GUESS' | 'UNCERTAIN' | 'CONFIDENT'
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'CASE' | 'ESSAY'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type FoundationLevel = 'BEGINNER' | 'BASIC' | 'INTERMEDIATE' | 'ADVANCED'

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
