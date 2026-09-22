import { api } from '@/api/client'
import type { AdminLoginResponse, AdminProfile } from '@/types/auth'

export function loginAdmin(username: string, password: string) {
  return api<AdminLoginResponse>('/api/v1/auth/admin/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentAdmin() {
  return api<AdminProfile>('/api/v1/admin/auth/me')
}

export function logoutAdmin() {
  return api<void>('/api/v1/admin/auth/logout', { method: 'POST' })
}
