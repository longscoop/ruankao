import test from 'node:test'
import assert from 'node:assert/strict'
import { buildAnswerValue, toggleChoice, toggleFavoriteIds } from '../src/lib/practice.ts'

test('single choice replaces previous selection while multiple choice toggles', () => {
  assert.deepEqual(toggleChoice(['A'], 'B', 'SINGLE_CHOICE'), ['B'])
  assert.deepEqual(toggleChoice(['A'], 'B', 'MULTIPLE_CHOICE'), ['A', 'B'])
  assert.deepEqual(toggleChoice(['A', 'B'], 'A', 'MULTIPLE_CHOICE'), ['B'])
})

test('multiple-choice answer serialization is stable', () => {
  assert.equal(buildAnswerValue(['C', 'A'], 'MULTIPLE_CHOICE'), 'A,C')
  assert.equal(buildAnswerValue(['B'], 'SINGLE_CHOICE'), 'B')
})

test('local favorite ids toggle without duplicates', () => {
  assert.deepEqual(toggleFavoriteIds([3, 7], 7), [3])
  assert.deepEqual(toggleFavoriteIds([3], 7), [3, 7])
  assert.deepEqual(toggleFavoriteIds([3, 3], 3), [])
})
