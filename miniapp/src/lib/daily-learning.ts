import type { DailyPlanDto, DailyTaskDto, StudyTaskType } from '@/types/api'

const LABELS: Record<StudyTaskType, string> = {
  WRONG_REVIEW: '复习错题',
  WEAK_KNOWLEDGE: '补强薄弱点',
  NEW_KNOWLEDGE: '学习新知识',
  REAL_EXAM: '练习真题',
}

export function taskLabel(type: StudyTaskType): string {
  return LABELS[type]
}

export function summarizePlan(plan: DailyPlanDto): { taskCount: number; plannedMinutes: number; targetMinutes: number } {
  return {
    taskCount: plan.tasks.length,
    plannedMinutes: plan.tasks.reduce((sum, task) => sum + Math.max(0, task.estimatedMinutes || 0), 0),
    targetMinutes: plan.targetMinutes,
  }
}

export function routeForTask(task: DailyTaskDto): string {
  if (task.taskType === 'WRONG_REVIEW') return '/pages/practice/wrong'
  if (task.taskType === 'REAL_EXAM') return '/pages/practice/index?source=REAL_EXAM'
  if (task.knowledgeId) return `/pages/knowledge/detail?id=${task.knowledgeId}`
  return '/pages/learning/today'
}
