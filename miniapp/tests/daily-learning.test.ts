import test from 'node:test'
import assert from 'node:assert/strict'
import { routeForTask, summarizePlan, taskLabel } from '../src/lib/daily-learning.ts'
import type { DailyPlanDto } from '../src/types/api.ts'

const plan: DailyPlanDto = {
  planId: 1,
  planDate: '2026-09-21',
  targetMinutes: 30,
  tasks: [
    { taskId: 1, taskType: 'WRONG_REVIEW', knowledgeId: 3, estimatedMinutes: 6, priorityScore: 80 },
    { taskId: 2, taskType: 'WEAK_KNOWLEDGE', knowledgeId: 7, estimatedMinutes: 11, priorityScore: 70 },
    { taskId: 3, taskType: 'REAL_EXAM', knowledgeId: null, estimatedMinutes: 6, priorityScore: null },
  ],
}

test('summarizes planned minutes without inventing completion', () => {
  assert.deepEqual(summarizePlan(plan), { taskCount: 3, plannedMinutes: 23, targetMinutes: 30 })
})

test('resolves task routes from server task type', () => {
  assert.equal(routeForTask(plan.tasks[0]), '/pages/practice/wrong')
  assert.equal(routeForTask(plan.tasks[1]), '/pages/knowledge/detail?id=7')
  assert.equal(routeForTask(plan.tasks[2]), '/pages/practice/index?source=REAL_EXAM')
})

test('has human labels for every V1 plan task type', () => {
  assert.equal(taskLabel('NEW_KNOWLEDGE'), '学习新知识')
})
