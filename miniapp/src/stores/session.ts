import { defineStore } from 'pinia'

const TOKEN_KEY = 'ruankao.session.token'
const USER_KEY = 'ruankao.session.userId'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: '' as string,
    userId: null as number | null,
    restored: false,
  }),
  actions: {
    restore() {
      this.token = uni.getStorageSync(TOKEN_KEY) || ''
      const storedUserId = Number(uni.getStorageSync(USER_KEY) || 0)
      this.userId = storedUserId > 0 ? storedUserId : null
      this.restored = true
    },
    setSession(token: string, userId: number) {
      this.token = token
      this.userId = userId
      uni.setStorageSync(TOKEN_KEY, token)
      uni.setStorageSync(USER_KEY, String(userId))
    },
    clear() {
      this.token = ''
      this.userId = null
      uni.removeStorageSync(TOKEN_KEY)
      uni.removeStorageSync(USER_KEY)
    },
  },
})
