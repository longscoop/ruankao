import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeRequestError } from '../src/lib/errors.ts'
import { validateExamProfile } from '../src/lib/profile.ts'
import { capabilityForPath } from '../src/config/capabilities.ts'

test('normalizes unauthorized HTTP errors', () => {
  assert.deepEqual(normalizeRequestError({ statusCode: 401 }), {
    code: 'UNAUTHORIZED',
    statusCode: 401,
    message: '登录状态已失效，请重新登录',
    retryable: false,
  })
})

test('marks gateway/server failures retryable', () => {
  assert.equal(normalizeRequestError({ statusCode: 503 }).retryable, true)
})

test('accepts only supported daily target minutes and future exam date', () => {
  assert.deepEqual(validateExamProfile({ examId: 1, examDate: '2026-11-01', dailyTargetMinutes: 30 }, '2026-09-21'), [])
  assert.ok(validateExamProfile({ examId: 0, examDate: '2026-09-20', dailyTargetMinutes: 20 }, '2026-09-21').length >= 3)
})

test('maps known API paths to backend phases', () => {
  assert.equal(capabilityForPath('/api/v1/learning/today').phase, 3)
  assert.equal(capabilityForPath('/api/v1/ai/chat').phase, 5)
  assert.equal(capabilityForPath('/api/v1/users/me/stats/weekly').phase, 7)
})


test('all miniapp page APIs are backed by the current server', () => {
  const paths = [
    '/api/v1/auth/wechat/login',
    '/api/v1/exams',
    '/api/v1/question-sessions',
    '/api/v1/users/me/wrong-questions',
    '/api/v1/courses',
    '/api/v1/videos/1',
    '/api/v1/ai/chat',
    '/api/v1/users/me/stats/weekly',
  ]
  for (const path of paths) {
    assert.equal(capabilityForPath(path).availableInCurrentServer, true, path)
  }
})
