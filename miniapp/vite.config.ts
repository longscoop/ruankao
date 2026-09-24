import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolveApiBaseUrl } from './src/lib/release'

export default defineConfig(({ command, mode }) => {
  if (command === 'build') {
    const buildEnv = loadEnv(mode, process.cwd(), 'VITE_')
    resolveApiBaseUrl(buildEnv.VITE_API_BASE_URL, true)
  }
  return { plugins: [uni()] }
})
