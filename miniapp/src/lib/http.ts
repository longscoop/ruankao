import { env } from '@/config/env'
import { normalizeRequestError, type AppRequestError } from '@/lib/errors'

export interface RequestOptions<T> {
  path: string
  method?: UniNamespace.RequestOptions['method']
  data?: unknown
  headers?: Record<string, string>
  token?: string
  timeout?: number
}

function requestId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

export async function request<T>(options: RequestOptions<T>): Promise<T> {
  try {
    const response = await uni.request({
      url: `${env.apiBaseUrl}${options.path}`,
      method: options.method || 'GET',
      data: options.data,
      timeout: options.timeout || env.timeoutMs,
      header: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-Request-Id': requestId(),
        ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
        ...options.headers,
      },
    })
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw { statusCode: response.statusCode, message: response.data }
    }
    return response.data as T
  } catch (error) {
    throw normalizeRequestError(error) satisfies AppRequestError
  }
}
