import test from 'node:test'
import assert from 'node:assert/strict'
import { validateApiBaseUrl } from '../src/lib/release.ts'

test('production API base URL must be a real HTTPS endpoint', () => {
  assert.equal(validateApiBaseUrl('https://api.ruankao.cn', true), null)
  assert.match(validateApiBaseUrl('http://api.ruankao.cn', true) || '', /HTTPS/)
  assert.match(validateApiBaseUrl('https://api.example.com', true) || '', /示例/)
})

test('localhost HTTP remains valid for local development', () => {
  assert.equal(validateApiBaseUrl('http://localhost:8080', false), null)
})
