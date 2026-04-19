import { apiClient } from './client'
import type { EquipmentSlot, ItemType } from './inventory'

export interface ShopItem {
  itemId: number
  itemCode: string
  itemName: string
  description: string | null
  type: ItemType
  equipmentSlot: EquipmentSlot | null
  baseAttack: number
  baseDefense: number
  price: number
  iconUrl: string | null
}

export interface ShopBuyResponse {
  goldBalance: number
  inventoryItemId: number
  quantity: number
  itemCode: string
  bought: number
}

export const shopApi = {
  list: () =>
    apiClient.get<ShopItem[]>('/shop').then(r => r.data),

  buy: (itemId: number, quantity: number = 1) =>
    apiClient.post<ShopBuyResponse>('/shop/buy', { itemId, quantity }).then(r => r.data),
}
