import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    networkOnline: true,
    lastRefreshAt: 0,
  }),
  actions: {
    markRefreshed() {
      this.lastRefreshAt = Date.now()
    },
    setNetworkOnline(online: boolean) {
      this.networkOnline = online
    },
  },
})
