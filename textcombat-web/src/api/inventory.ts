import { apiClient } from './client'

export type ItemType = 'CONSUMABLE' | 'EQUIPMENT' | 'MATERIAL'
export type EquipmentSlot = 'WEAPON' | 'ARMOR' | 'HELMET' | 'BOOTS' | 'ACCESSORY'

export interface InventoryItem {
  inventoryItemId: number
  itemCode: string
  itemName: string
  description: string | null
  type: ItemType
  equipmentSlot: EquipmentSlot | null    // 僅用於顯示分類
  quantity: number
  enhancementLevel: number
  baseAttack: number
  baseDefense: number
  iconUrl: string | null
  equipped: boolean
  equippedSlotIndex: number | null       // 1~9 或 null
}

export interface InventoryResponse {
  // key 是 "1" ~ "9"（JSON 序列化後 number key 變字串）；值可能是 null
  slots: Record<string, InventoryItem | null>
  items: InventoryItem[]
  totalAttack: number
  totalDefense: number
}

export interface UseResponse {
  message: string
  inventory: InventoryResponse
}

export const inventoryApi = {
  list: () =>
    apiClient.get<InventoryResponse>('/inventory').then(r => r.data),

  // slotIndex 不填 → 後端自動放第一格空位
  equip: (inventoryItemId: number, slotIndex?: number) =>
    apiClient.post<InventoryResponse>('/inventory/equip', { inventoryItemId, slotIndex }).then(r => r.data),

  unequip: (slotIndex: number) =>
    apiClient.post<InventoryResponse>('/inventory/unequip', { slotIndex }).then(r => r.data),

  use: (slotIndex: number) =>
    apiClient.post<UseResponse>('/inventory/use', { slotIndex }).then(r => r.data),

  discard: (inventoryItemId: number, quantity: number) =>
    apiClient.post<InventoryResponse>('/inventory/discard', { inventoryItemId, quantity }).then(r => r.data),
}
