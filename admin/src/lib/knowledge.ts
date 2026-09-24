export interface PendingTurn { sessionId: string; message: string; requestId: string }

export function validateMaterial(filename: string, size: number): void {
  if (!/\.(pdf|txt|md)$/i.test(filename)) throw new Error('仅支持文本型 PDF、TXT 和 Markdown')
  if (!Number.isFinite(size) || size <= 0 || size > 20 * 1024 * 1024) throw new Error('文件不能为空且不能超过 20 MiB')
}

/** Request correlation only; this identifier is never an authentication credential. */
export function newRequestId(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, value => {
    const random = Math.floor(Math.random() * 16)
    return (value === 'x' ? random : (random & 3) | 8).toString(16)
  })
}

export function prepareTurn(pending: PendingTurn | undefined, sessionId: string, message: string,
  makeId: () => string = newRequestId): PendingTurn {
  const text = message.trim()
  if (!sessionId || !text || text.length > 2000 || text.includes('\u0000')) throw new Error('请先新建或选择会话，问题须为 1 至 2000 字符')
  if (pending?.sessionId === sessionId && pending.message === text) return pending
  return { sessionId, message: text, requestId: makeId() }
}

export function sourceLabel(source: { filename: string; page: number }): string {
  return source.filename + (/\.pdf$/i.test(source.filename) ? ` · 第 ${source.page} 页` : ' · 文本片段')
}

export function answerLabel(status: 'GROUNDED' | 'EXTRACT_ONLY' | 'NO_EVIDENCE'): string {
  return { GROUNDED: '有来源的 AI 回答', EXTRACT_ONLY: '仅原文摘录：引用校验未通过', NO_EVIDENCE: '资料不足' }[status]
}
