import { apiRequest } from '@/api/client'
import type { DailyPlanDto } from '@/types/api'

export function getTodayPlan(): Promise<DailyPlanDto> {
  return apiRequest<DailyPlanDto>({ path: '/api/v1/learning/today' })
}
