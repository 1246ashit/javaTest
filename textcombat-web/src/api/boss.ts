import { apiClient } from './client'

export interface Boss {
  id: number
  code: string
  name: string
  description: string | null
  icon: string | null
  hp: number
  attack: number
  defense: number
  rewardGold: number
}

export const bossApi = {
  list: () => apiClient.get<Boss[]>('/bosses').then(r => r.data),
}
