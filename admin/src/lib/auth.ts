export function normalizeBearerToken(value: string | null | undefined): string {
  return (value || '').trim().replace(/^Bearer\s+/i, '')
}

export function apiErrorMessage(status: number, body: string): string {
  const trimmed = body.trim()
  if (trimmed) {
    try {
      const parsed = JSON.parse(trimmed) as { message?: unknown; error?: unknown }
      if (typeof parsed.message === 'string' && parsed.message.trim()) {
        return parsed.message.trim()
      }
      if (typeof parsed.error === 'string' && parsed.error.trim()) {
        return parsed.error.trim()
      }
    } catch {
      if (trimmed.length <= 180) return trimmed
    }
  }

  if (status === 401) return '用户名或密码错误，或登录状态已失效'
  if (status === 403) return '当前账号没有管理员权限'
  if (status >= 500) return '服务暂时不可用，请稍后重试'
  return '请求失败（HTTP ' + status + '）'
}
