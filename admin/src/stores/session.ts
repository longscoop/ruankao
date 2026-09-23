import { defineStore } from 'pinia'

const KEY = 'ruankao.admin.bearer'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem(KEY) || '',
  }),
  actions: {
    setToken(token: string) {
      this.token = token.trim().replace(/^Bearer\s+/i, '')
      if (this.token) localStorage.setItem(KEY, this.token)
      else localStorage.removeItem(KEY)
    },
    clear() {
      this.token = ''
      localStorage.removeItem(KEY)
    },
  },
})
