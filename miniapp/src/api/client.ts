import { request, type RequestOptions } from '@/lib/http'
import { useSessionStore } from '@/stores/session'

export function apiRequest<T>(options: RequestOptions<T>): Promise<T> {
  const session = useSessionStore()
  return request<T>({ ...options, token: session.token || undefined })
}
