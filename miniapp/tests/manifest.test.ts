import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

test('WeChat miniapp enables component on-demand injection', () => {
  const manifest = JSON.parse(readFileSync(new URL('../src/manifest.json', import.meta.url), 'utf8'))
  assert.equal(manifest['mp-weixin'].lazyCodeLoading, 'requiredComponents')
})
