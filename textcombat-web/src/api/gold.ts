import { apiClient } from './client'

export interface UserGoldResponse {
  gold: number
}

export const goldApi = {
  get: () =>
    apiClient.get<UserGoldResponse>('/gold').then(r => r.data),

  // 管理員用：給某個玩家金幣（amount 可正可負）
  grant: (userId: number, amount: number, note?: string) =>
    apiClient.post<{ userId: number; amount: number; balance: number }>(
      '/gold/grant',
      { userId, amount, note }
    ).then(r => r.data),
}
