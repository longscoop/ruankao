import test from 'node:test'
import assert from 'node:assert/strict'
import { validateMaterial, prepareTurn, newRequestId, sourceLabel, answerLabel } from '../src/lib/knowledge.ts'

test('only supported nonempty bounded materials can be uploaded', () => {
  for (const name of ['架构.PDF', 'notes.txt', 'notes.MD']) assert.doesNotThrow(() => validateMaterial(name, 20 * 1024 * 1024))
  for (const [name, size] of [['bad.exe', 10], ['a.pdf', 0], ['a.pdf', 20 * 1024 * 1024 + 1], ['a.pdf', NaN]] as const) {
    assert.throws(() => validateMaterial(name, size))
  }
})

test('retry preserves request identity while changed message or session gets a new one', () => {
  const pending = prepareTurn(undefined, 'session-a', ' 分层架构？ ', () => 'request-a')
  assert.equal(pending.message, '分层架构？')
  assert.equal(prepareTurn(pending, 'session-a', '分层架构？', () => 'unused'), pending)
  assert.equal(prepareTurn(pending, 'session-b', '分层架构？', () => 'request-b').requestId, 'request-b')
  assert.equal(prepareTurn(pending, 'session-a', '为什么？', () => 'request-c').requestId, 'request-c')
  assert.throws(() => prepareTurn(undefined, '', '问题'))
  for (const text of [' ', '字'.repeat(2001), '问\u0000题']) assert.throws(() => prepareTurn(undefined, 's', text))
})

test('request ids have UUID shape and are not reused', () => {
  const ids = Array.from({ length: 100 }, () => newRequestId())
  assert.equal(new Set(ids).size, 100)
  for (const id of ids) assert.match(id, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
})

test('source labels distinguish PDF pages from unpaginated text and answer fallbacks', () => {
  assert.equal(sourceLabel({ filename: '架构.PDF', page: 7 }), '架构.PDF · 第 7 页')
  assert.equal(sourceLabel({ filename: '笔记.md', page: 1 }), '笔记.md · 文本片段')
  assert.equal(answerLabel('GROUNDED'), '有来源的 AI 回答')
  assert.equal(answerLabel('EXTRACT_ONLY'), '仅原文摘录：引用校验未通过')
  assert.equal(answerLabel('NO_EVIDENCE'), '资料不足')
})
