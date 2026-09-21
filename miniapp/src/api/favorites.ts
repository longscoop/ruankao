import { apiRequest } from '@/api/client'
import type { LocalFavoriteQuestion } from '@/types/api'

export function listFavorites(): Promise<LocalFavoriteQuestion[]> {
  return apiRequest<LocalFavoriteQuestion[]>({ path: '/api/v1/users/me/favorites' })
}

export function addFavorite(questionId: number): Promise<void> {
  return apiRequest<void>({
    path: `/api/v1/users/me/favorites/${questionId}`,
    method: 'PUT',
  })
}

export function removeFavorite(questionId: number): Promise<void> {
  return apiRequest<void>({
    path: `/api/v1/users/me/favorites/${questionId}`,
    method: 'DELETE',
  })
}
