export type RequestErrorCode = 'UNAUTHORIZED' | 'FORBIDDEN' | 'NOT_FOUND' | 'EXAM_PROFILE_REQUIRED' | 'NETWORK' | 'SERVER' | 'UNKNOWN'

export interface AppRequestError {
  code: RequestErrorCode
  statusCode?: number
  message: string
  retryable: boolean
}

type ErrorLike = {
  statusCode?: number
  errMsg?: string
  message?: unknown
}

export function normalizeRequestError(error: unknown): AppRequestError {
  const value = (error && typeof error === 'object' ? error : {}) as ErrorLike
  const statusCode = value.statusCode
  const response = value.message && typeof value.message === 'object'
    ? value.message as { code?: string; message?: string }
    : undefined
  if (statusCode === 401) return { code: 'UNAUTHORIZED', statusCode, message: '登录状态已失效，请重新登录', retryable: false }
  if (statusCode === 403) return { code: 'FORBIDDEN', statusCode, message: '当前账号没有访问权限', retryable: false }
  if (statusCode === 404 && response?.code === 'EXAM_PROFILE_REQUIRED') {
    return { code: 'EXAM_PROFILE_REQUIRED', statusCode, message: response.message || '请先设置目标考试', retryable: false }
  }
  if (statusCode === 404) return { code: 'NOT_FOUND', statusCode, message: '请求的内容暂不存在', retryable: false }
  if (statusCode && statusCode >= 500) return { code: 'SERVER', statusCode, message: '服务暂时不可用，请稍后重试', retryable: true }
  if (value.errMsg?.includes('timeout') || value.errMsg?.includes('fail')) {
    return { code: 'NETWORK', message: '网络连接失败，请检查网络后重试', retryable: true }
  }
  return { code: 'UNKNOWN', statusCode, message: typeof value.message === 'string' ? value.message : '请求失败，请稍后重试', retryable: false }
}
