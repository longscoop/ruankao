import test from 'node:test'
import assert from 'node:assert/strict'
import { prepareTurn, newRequestId, sourceLabel, answerLabel } from '../src/lib/agents.ts'

test('network retry keeps the same question and UUID without duplicating a turn', () => {
  const pending = prepareTurn(undefined, 's1', ' 分层架构？ ', () => 'id1')
  assert.equal(pending.message, '分层架构？')
  assert.equal(prepareTurn(pending, 's1', '分层架构？', () => 'unused'), pending)
  assert.equal(prepareTurn(pending, 's2', '分层架构？', () => 'id2').requestId, 'id2')
  assert.equal(prepareTurn(pending, 's1', '继续解释', () => 'id3').requestId, 'id3')
})

test('questions require a session, nonempty text, no NUL and at most 2000 characters', () => {
  assert.throws(() => prepareTurn(undefined, '', '问题'))
  for (const message of [' ', '字'.repeat(2001), '问\u0000题']) assert.throws(() => prepareTurn(undefined, 's', message))
  assert.equal(prepareTurn(undefined, 's', '字'.repeat(2000)).message.length, 2000)
})

test('portable request identifiers are UUIDs and unique within a batch', () => {
  const ids = Array.from({ length: 100 }, () => newRequestId())
  assert.equal(new Set(ids).size, 100)
  ids.forEach(id => assert.match(id, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/))
})

test('citations do not invent page numbers for text and fallback states are explicit', () => {
  assert.equal(sourceLabel({ filename: '讲义.pdf', page: 12 }), '讲义.pdf · 第 12 页')
  assert.equal(sourceLabel({ filename: 'notes.txt', page: 1 }), 'notes.txt · 文本片段')
  assert.equal(answerLabel('EXTRACT_ONLY'), '仅原文摘录：引用校验未通过')
  assert.equal(answerLabel('NO_EVIDENCE'), '资料不足')
  assert.equal(answerLabel('GROUNDED'), '有来源的 AI 回答')
})
