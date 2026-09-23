import test from 'node:test'
import assert from 'node:assert/strict'
import { parseKnowledgeIds, parseQuestionLinks, itemNeedsConfirm } from '../src/lib/import-review.ts'

test('parses distinct positive knowledge ids', () => {
  assert.deepEqual(parseKnowledgeIds('12, 7,12, 9'), [12, 7, 9])
  assert.deepEqual(parseKnowledgeIds('0,-1,foo'), [])
})

test('builds a primary question knowledge link without inventing weights', () => {
  assert.deepEqual(parseQuestionLinks('42'), [
    { knowledgeId: 42, weight: 1, primary: true },
  ])
  assert.deepEqual(parseQuestionLinks(''), [])
})

test('only approved lesson items need batch confirm before publish', () => {
  assert.equal(itemNeedsConfirm({ itemType: 'LESSON', status: 'APPROVED', targetId: null }), true)
  assert.equal(itemNeedsConfirm({ itemType: 'QUESTION', status: 'APPROVED', targetId: null }), false)
  assert.equal(itemNeedsConfirm({ itemType: 'LESSON', status: 'MATERIALIZED', targetId: 5 }), false)
})
