export interface ExamProfileDraft {
  examId: number
  examDate: string
  dailyTargetMinutes: number
}

const TARGETS = new Set([15, 30, 60, 90])

export function validateExamProfile(profile: ExamProfileDraft, today: string): string[] {
  const errors: string[] = []
  if (!Number.isInteger(profile.examId) || profile.examId <= 0) errors.push('请选择目标考试')
  if (!/^\d{4}-\d{2}-\d{2}$/.test(profile.examDate) || profile.examDate < today) errors.push('请选择今天或之后的考试日期')
  if (!TARGETS.has(profile.dailyTargetMinutes)) errors.push('每日学习时长仅支持 15/30/60/90 分钟')
  return errors
}
