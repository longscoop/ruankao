import test from 'node:test'
import assert from 'node:assert/strict'
import { formatDuration, isVideoComplete, shouldReportProgress, videoProgressPercent } from '../src/lib/video.ts'

test('video completion requires at least 85 percent watched', () => {
  assert.equal(videoProgressPercent(85, 100), 85)
  assert.equal(isVideoComplete(84.9, 100), false)
  assert.equal(isVideoComplete(85, 100), true)
  assert.equal(videoProgressPercent(180, 100), 100)
})

test('progress reports are throttled but final progress is always reported', () => {
  assert.equal(shouldReportProgress(10, 20, 100), false)
  assert.equal(shouldReportProgress(10, 26, 100), true)
  assert.equal(shouldReportProgress(90, 100, 100), true)
})

test('formats durations for learner-facing labels', () => {
  assert.equal(formatDuration(59), '0:59')
  assert.equal(formatDuration(61), '1:01')
  assert.equal(formatDuration(3661), '1:01:01')
})
