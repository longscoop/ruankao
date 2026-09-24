import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveApiBaseUrl, validateApiBaseUrl } from '../src/lib/release.ts'

test('production build defaults to the deployed HTTPS API domain', () => {
  assert.equal(resolveApiBaseUrl(undefined, true), 'https://www.e68q.cn')
  assert.equal(resolveApiBaseUrl('', true), 'https://www.e68q.cn')
  assert.equal(resolveApiBaseUrl('https://staging.e68q.cn/', true), 'https://staging.e68q.cn')
  assert.equal(resolveApiBaseUrl(undefined, false), 'http://localhost:8080')
})

test('production build rejects an invalid API override', () => {
  assert.throws(() => resolveApiBaseUrl('http://localhost:8080', true), /HTTPS/)
  assert.throws(() => resolveApiBaseUrl('https://api.example.com', true), /示例/)
  assert.throws(() => resolveApiBaseUrl('https://localhost', true), /域名/)
  assert.throws(() => resolveApiBaseUrl('https://43.143.201.211', true), /域名/)
})

test('production API base URL must be a real HTTPS endpoint', () => {
  assert.equal(validateApiBaseUrl('https://api.ruankao.cn', true), null)
  assert.match(validateApiBaseUrl('http://api.ruankao.cn', true) || '', /HTTPS/)
  assert.match(validateApiBaseUrl('https://api.example.com', true) || '', /示例/)
  assert.match(validateApiBaseUrl('https://localhost', true) || '', /域名/)
  assert.match(validateApiBaseUrl('https://43.143.201.211', true) || '', /域名/)
})

test('localhost HTTP remains valid for local development', () => {
  assert.equal(validateApiBaseUrl('http://localhost:8080', false), null)
})
