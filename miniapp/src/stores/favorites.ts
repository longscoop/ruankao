import { defineStore } from 'pinia'
import { addFavorite, listFavorites, removeFavorite } from '@/api/favorites'
import type { LocalFavoriteQuestion, PracticeQuestionDto } from '@/types/api'

const KEY = 'ruankao.favorite-cache.v2'

export const useFavoritesStore = defineStore('favorites', {
  state: () => ({
    items: [] as LocalFavoriteQuestion[],
    restored: false,
    syncing: false,
  }),
  actions: {
    restore() {
      const stored = uni.getStorageSync(KEY)
      this.items = Array.isArray(stored) ? stored : []
      this.restored = true
    },
    has(questionId: number) {
      return this.items.some((item) => item.question.id === questionId)
    },
    async sync() {
      if (!this.restored) this.restore()
      this.syncing = true
      try {
        this.items = await listFavorites()
        uni.setStorageSync(KEY, this.items)
      } finally {
        this.syncing = false
      }
    },
    async toggle(question: PracticeQuestionDto) {
      if (!this.restored) this.restore()
      if (this.has(question.id)) {
        await removeFavorite(question.id)
        this.items = this.items.filter((item) => item.question.id !== question.id)
      } else {
        await addFavorite(question.id)
        this.items = [...this.items, { question, savedAt: new Date().toISOString() }]
      }
      uni.setStorageSync(KEY, this.items)
    },
    clearCache() {
      this.items = []
      uni.removeStorageSync(KEY)
    },
  },
})
