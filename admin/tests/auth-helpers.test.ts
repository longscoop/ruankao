import test from 'node:test'
import assert from 'node:assert/strict'
import { apiErrorMessage, normalizeBearerToken } from '../src/lib/auth.ts'

test('normalizes bearer tokens before persistence', () => {
  assert.equal(normalizeBearerToken(' Bearer abc-123 '), 'abc-123')
  assert.equal(normalizeBearerToken('abc-123'), 'abc-123')
  assert.equal(normalizeBearerToken(null), '')
})

test('prefers structured API error messages', () => {
  assert.equal(
    apiErrorMessage(401, '{"message":"登录状态无效或已过期"}'),
    '登录状态无效或已过期',
  )
  assert.equal(apiErrorMessage(403, ''), '当前账号没有管理员权限')
  assert.equal(apiErrorMessage(503, ''), '服务暂时不可用，请稍后重试')
})
