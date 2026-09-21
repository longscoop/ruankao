import test from 'node:test'
import assert from 'node:assert/strict'
import { assessmentProgress, canSubmitAssessmentAnswer } from '../src/lib/assessment.ts'
import { validateExamProfile } from '../src/lib/profile.ts'

test('assessment progress is clamped and uses one-based display', () => {
  assert.deepEqual(assessmentProgress(0, 10), { current: 1, total: 10, percent: 10 })
  assert.deepEqual(assessmentProgress(12, 10), { current: 10, total: 10, percent: 100 })
})

test('assessment answer requires a question and a non-empty answer', () => {
  assert.equal(canSubmitAssessmentAnswer(10, 'A'), true)
  assert.equal(canSubmitAssessmentAnswer(0, 'A'), false)
  assert.equal(canSubmitAssessmentAnswer(10, '  '), false)
})

test('profile permits today as exam date', () => {
  assert.deepEqual(validateExamProfile({ examId: 8, examDate: '2026-09-21', dailyTargetMinutes: 15 }, '2026-09-21'), [])
})
