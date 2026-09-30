import { resolveApiBaseUrl } from '@/lib/release'

const rawTimeout = Number(import.meta.env.VITE_API_TIMEOUT_MS || 10000)

export const env = {
  apiBaseUrl: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL, import.meta.env.PROD),
  timeoutMs: Number.isFinite(rawTimeout) && rawTimeout > 0 ? rawTimeout : 10000,
}
