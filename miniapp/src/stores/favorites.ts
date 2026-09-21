import { defineStore } from 'pinia'
import type { LocalFavoriteQuestion, PracticeQuestionDto } from '@/types/api'

const KEY = 'ruankao.local-favorites.v1'

export const useFavoritesStore = defineStore('favorites', {
  state: () => ({ items: [] as LocalFavoriteQuestion[], restored: false }),
  actions: {
    restore() {
      const stored = uni.getStorageSync(KEY)
      this.items = Array.isArray(stored) ? stored : []
      this.restored = true
    },
    has(questionId: number) { return this.items.some((item) => item.question.id === questionId) },
    toggle(question: PracticeQuestionDto) {
      if (!this.restored) this.restore()
      if (this.has(question.id)) this.items = this.items.filter((item) => item.question.id !== question.id)
      else this.items = [...this.items, { question, savedAt: new Date().toISOString() }]
      uni.setStorageSync(KEY, this.items)
    },
  },
})
