import { apiClient } from './client'
import type { UserInfo } from './auth'

export interface GoldTransaction {
  id: number
  userId: number
  username: string
  amount: number
  balanceAfter: number
  reason: string
  refId: string | null
  note: string | null
  createdAt: string
}

export const adminApi = {
  listUsers: () =>
    apiClient.get<UserInfo[]>('/users').then(r => r.data),

  listTransactions: (userId?: number) =>
    apiClient.get<GoldTransaction[]>('/gold/transactions', {
      params: userId ? { userId } : undefined,
    }).then(r => r.data),
}
