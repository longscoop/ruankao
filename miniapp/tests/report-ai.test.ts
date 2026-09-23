import test from 'node:test'
import assert from 'node:assert/strict'
import { formatMastery, weeklyLearningLabel } from '../src/lib/report.ts'

test('mastery without evidence is pending assessment', () => {
  assert.equal(formatMastery(null, 0), '待评估')
  assert.equal(formatMastery(0, 0), '待评估')
  assert.equal(formatMastery(72.6, 4), '73%')
})

test('weekly learning label uses real server minutes only', () => {
  assert.equal(weeklyLearningLabel(0), '本周暂无学习记录')
  assert.equal(weeklyLearningLabel(95), '本周已学习 1 小时 35 分钟')
})
