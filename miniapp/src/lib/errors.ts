export type RequestErrorCode = 'UNAUTHORIZED' | 'FORBIDDEN' | 'NOT_FOUND' | 'NETWORK' | 'SERVER' | 'UNKNOWN'

export interface AppRequestError {
  code: RequestErrorCode
  statusCode?: number
  message: string
  retryable: boolean
}

type ErrorLike = {
  statusCode?: number
  errMsg?: string
  message?: string
}

export function normalizeRequestError(error: unknown): AppRequestError {
  const value = (error && typeof error === 'object' ? error : {}) as ErrorLike
  const statusCode = value.statusCode
  if (statusCode === 401) return { code: 'UNAUTHORIZED', statusCode, message: '登录状态已失效，请重新登录', retryable: false }
  if (statusCode === 403) return { code: 'FORBIDDEN', statusCode, message: '当前账号没有访问权限', retryable: false }
  if (statusCode === 404) return { code: 'NOT_FOUND', statusCode, message: '请求的内容暂不存在', retryable: false }
  if (statusCode && statusCode >= 500) return { code: 'SERVER', statusCode, message: '服务暂时不可用，请稍后重试', retryable: true }
  if (value.errMsg?.includes('timeout') || value.errMsg?.includes('fail')) {
    return { code: 'NETWORK', message: '网络连接失败，请检查网络后重试', retryable: true }
  }
  return { code: 'UNKNOWN', statusCode, message: value.message || '请求失败，请稍后重试', retryable: false }
}
