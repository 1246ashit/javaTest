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

// Spring Data JPA 的分頁回應格式
interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const adminApi = {
  listUsers: () =>
    apiClient.get<UserInfo[]>('/users').then(r => r.data),

  listTransactions: (userId?: number) =>
    apiClient.get<SpringPage<GoldTransaction>>('/gold/transactions', {
      params: {
        ...(userId ? { userId } : {}),
        size: 1000,
      },
    }).then(r => r.data.content),
}
