import { defineStore } from 'pinia'
import { normalizeBearerToken } from '@/lib/auth'
import type { AdminProfile } from '@/types/auth'

const TOKEN_KEY = 'ruankao.admin.bearer'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: normalizeBearerToken(localStorage.getItem(TOKEN_KEY)),
    admin: null as AdminProfile | null,
  }),
  getters: {
    isAuthenticated: state => Boolean(state.token),
  },
  actions: {
    setSession(token: string, admin: AdminProfile) {
      this.token = normalizeBearerToken(token)
      this.admin = admin
      if (this.token) localStorage.setItem(TOKEN_KEY, this.token)
      else localStorage.removeItem(TOKEN_KEY)
    },
    setAdmin(admin: AdminProfile) {
      this.admin = admin
    },
    clear() {
      this.token = ''
      this.admin = null
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})
