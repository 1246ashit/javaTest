import { apiClient } from './client'

export type RoomPhase = 'PLAYER' | 'BOSS'
export type RoomOutcome = 'ONGOING' | 'VICTORY' | 'DEFEAT' | 'ABANDONED'
export type RoomActionType = 'ATTACK' | 'USE_POTION' | 'SKIP'

export interface RoomMember {
  userId: number
  username: string
  displayName: string | null
  hp: number
  maxHp: number
  attack: number
  defense: number
  alive: boolean
  fled: boolean
  actedThisRound: boolean
  joinedAt: number
}

export interface Room {
  roomId: string
  name: string
  bossId: number
  bossCode: string
  bossName: string
  bossIcon: string | null
  bossMaxHp: number
  bossHp: number
  bossAttack: number
  bossDefense: number
  bossRewardGold: number

  members: RoomMember[]
  round: number
  phase: RoomPhase
  outcome: RoomOutcome
  log: string[]
  createdAt: number
  endedAt: number | null
}

export interface RoomSummary {
  roomId: string
  name: string
  bossName: string
  bossIcon: string | null
  bossHp: number
  bossMaxHp: number
  memberCount: number
  maxMembers: number
  outcome: RoomOutcome
  createdAt: number
}

export const roomApi = {
  list: () =>
    apiClient.get<RoomSummary[]>('/rooms').then(r => r.data),

  create: (bossId: number, name?: string) =>
    apiClient.post<Room>('/rooms', { bossId, name }).then(r => r.data),

  get: (roomId: string) =>
    apiClient.get<Room>(`/rooms/${roomId}`).then(r => r.data),

  join: (roomId: string) =>
    apiClient.post<Room>(`/rooms/${roomId}/join`).then(r => r.data),

  leave: (roomId: string) =>
    apiClient.post<Room>(`/rooms/${roomId}/leave`).then(r => r.data),

  action: (roomId: string, action: RoomActionType, inventoryItemId?: number) =>
    apiClient.post<Room>(`/rooms/${roomId}/action`, { action, inventoryItemId }).then(r => r.data),
}
