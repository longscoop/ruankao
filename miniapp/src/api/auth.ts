import { request } from '@/lib/http'
import type { WechatLoginResponse } from '@/types/api'

export function loginWithWechat(code: string): Promise<WechatLoginResponse> {
  return request<WechatLoginResponse>({
    path: '/api/v1/auth/wechat/login',
    method: 'POST',
    data: { code },
  })
}
