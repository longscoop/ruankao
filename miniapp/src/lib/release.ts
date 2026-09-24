export function resolveApiBaseUrl(value: string | undefined, production: boolean): string {
  const baseUrl = value?.trim() || (production ? 'https://www.e68q.cn' : 'http://localhost:8080')
  const error = validateApiBaseUrl(baseUrl, production)
  if (error) throw new Error(error)
  return baseUrl.replace(/\/$/, '')
}

export function validateApiBaseUrl(value: string, production: boolean): string | null {
  const text = value.trim()
  if (!text) return 'API 地址不能为空'
  let url: URL
  try { url = new URL(text) } catch { return 'API 地址格式无效' }
  if (production && url.protocol !== 'https:') return '生产环境 API 必须使用 HTTPS'
  if (production && (url.hostname === 'api.example.com' || url.hostname.endsWith('.example.com'))) return '生产环境不能使用示例 API 地址'
  if (production && (url.hostname === 'localhost' || url.hostname.endsWith('.localhost') || /^\d+\.\d+\.\d+\.\d+$/.test(url.hostname) || url.hostname.startsWith('['))) return '生产环境 API 必须使用正式域名'
  return null
}
