export function videoProgressPercent(progressSeconds: number, durationSeconds: number): number {
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) return 0
  const progress = Math.max(0, Math.min(progressSeconds, durationSeconds))
  return Math.round((progress / durationSeconds) * 10000) / 100
}

export function isVideoComplete(progressSeconds: number, durationSeconds: number): boolean {
  return videoProgressPercent(progressSeconds, durationSeconds) >= 85
}

export function shouldReportProgress(lastReportedSeconds: number, currentSeconds: number, durationSeconds: number, intervalSeconds = 15): boolean {
  if (durationSeconds > 0 && currentSeconds >= durationSeconds) return true
  return currentSeconds - lastReportedSeconds >= intervalSeconds
}

export function formatDuration(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds || 0))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remain = seconds % 60
  const mm = hours > 0 ? String(minutes).padStart(2, '0') : String(minutes)
  const ss = String(remain).padStart(2, '0')
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`
}
