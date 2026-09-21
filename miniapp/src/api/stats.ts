import { apiRequest } from '@/api/client'
import type { WeeklyStatsDto } from '@/types/api'

export function getWeeklyStats(): Promise<WeeklyStatsDto> {
  return apiRequest<WeeklyStatsDto>({ path: '/api/v1/users/me/stats/weekly' })
}
