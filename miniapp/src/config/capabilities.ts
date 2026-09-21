export interface ApiCapability {
  phase: number
  availableInCurrentServer: boolean
  label: string
}

const RULES: Array<[RegExp, ApiCapability]> = [
  [/^\/api\/v1\/auth\/wechat\/login$/, { phase: 7, availableInCurrentServer: false, label: '微信登录' }],
  [/^\/api\/v1\/exams$/, { phase: 7, availableInCurrentServer: false, label: '考试目录' }],
  [/^\/api\/v1\/learning\/today$/, { phase: 3, availableInCurrentServer: true, label: '今日学习' }],
  [/^\/api\/v1\/assessments/, { phase: 3, availableInCurrentServer: true, label: '摸底测试' }],
  [/^\/api\/v1\/users\/me\/exam-profile$/, { phase: 3, availableInCurrentServer: true, label: '考试档案' }],
  [/^\/api\/v1\/courses/, { phase: 4, availableInCurrentServer: false, label: '课程接口' }],
  [/^\/api\/v1\/videos/, { phase: 4, availableInCurrentServer: false, label: '视频接口' }],
  [/^\/api\/v1\/ai\//, { phase: 5, availableInCurrentServer: false, label: 'AI 能力' }],
  [/^\/api\/v1\/users\/me\/stats\/weekly$/, { phase: 7, availableInCurrentServer: false, label: '周报接口' }],
]

export function capabilityForPath(path: string): ApiCapability {
  return RULES.find(([rule]) => rule.test(path))?.[1] ?? { phase: 7, availableInCurrentServer: false, label: '待接入接口' }
}
