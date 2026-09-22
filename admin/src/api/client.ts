import { useSessionStore } from '@/stores/session'
import { apiErrorMessage } from '@/lib/auth'

const baseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const session = useSessionStore()
  const response = await fetch(baseUrl + path, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(session.token ? { Authorization: 'Bearer ' + session.token } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const text = await response.text()
    const message = apiErrorMessage(response.status, text)
    const hadSession = Boolean(session.token)
    if (response.status === 401 && hadSession) {
      session.clear()
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new Event('ruankao:unauthorized'))
      }
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
