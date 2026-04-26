import { apiClient } from './client'
import type { InventoryResponse } from './inventory'

export type EnhanceFailEffect = 'NOTHING' | 'DOWNGRADE' | 'DESTROY'

export interface MaterialRequirement {
  itemId: number
  itemCode: string
  itemName: string
  quantityNeeded: number
  quantityOwned: number
}

export interface EnhancePreview {
  inventoryItemId: number
  itemCode: string
  itemName: string
  currentLevel: number
  maxLevel: number
  currentAttack: number
  currentDefense: number
  nextAttack: number
  nextDefense: number
  successRate: number              // 0 ~ 1
  goldCost: number
  goldOwned: number
  materialCosts: MaterialRequirement[]
  onFail: EnhanceFailEffect
  canEnhance: boolean
  blockReason: string | null       // canEnhance=false 時帶原因（已滿、素材不足、金幣不足…）
}

export interface EnhanceResult {
  success: boolean
  previousLevel: number
  newLevel: number                 // 失敗 + DOWNGRADE 可能比 previous 低；DESTROY 時 -1
  destroyed: boolean               // 武器被銷毀（背包中已不存在）
  goldSpent: number
  goldBalance: number
  message: string                  // 後端組好的中文訊息（"強化成功！+3 → +4"）
  inventory: InventoryResponse     // 強化後的完整背包，前端直接覆蓋
}

export const enhanceApi = {
  preview: (inventoryItemId: number) =>
    apiClient.get<EnhancePreview>(`/enhance/preview/${inventoryItemId}`).then(r => r.data),

  enhance: (inventoryItemId: number) =>
    apiClient.post<EnhanceResult>('/enhance', { inventoryItemId }).then(r => r.data),
}
