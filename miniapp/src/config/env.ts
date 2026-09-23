const rawTimeout = Number(import.meta.env.VITE_API_TIMEOUT_MS || 10000)

export const env = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, ''),
  timeoutMs: Number.isFinite(rawTimeout) && rawTimeout > 0 ? rawTimeout : 10000,
}
