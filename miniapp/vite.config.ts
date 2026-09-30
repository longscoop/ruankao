import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolveApiBaseUrl, validateApiBaseUrl } from './src/lib/release'

export default defineConfig(({ command, mode }) => {
  if (command === 'build') {
    const buildEnv = loadEnv(mode, process.cwd(), 'VITE_')
    const baseUrl = resolveApiBaseUrl(buildEnv.VITE_API_BASE_URL, true)
    const error = validateApiBaseUrl(baseUrl, true)
    if (error) throw new Error(error)
  }
  return { plugins: [uni()] }
})
