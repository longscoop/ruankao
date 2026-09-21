export function formatMastery(score: number | null | undefined, evidenceCount: number): string {
  if (!evidenceCount || score == null) return '待评估'
  return `${Math.round(Math.max(0, Math.min(100, score)))}%`
}

export function weeklyLearningLabel(minutes: number): string {
  const safe = Math.max(0, Math.floor(minutes || 0))
  if (safe === 0) return '本周暂无学习记录'
  const hours = Math.floor(safe / 60)
  const remain = safe % 60
  if (hours === 0) return `本周已学习 ${remain} 分钟`
  if (remain === 0) return `本周已学习 ${hours} 小时`
  return `本周已学习 ${hours} 小时 ${remain} 分钟`
}
