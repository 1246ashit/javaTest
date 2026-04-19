import { apiClient } from './client'

export interface UserInfo {
  id: number
  username: string
  displayName: string | null
  gold: number
  roles: string[]
  permissions: string[]
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post<LoginResponse>('/auth/login', { username, password }).then(r => r.data),

  register: (username: string, password: string, displayName?: string) =>
    apiClient.post<UserInfo>('/auth/register', { username, password, displayName }).then(r => r.data),

  me: () => apiClient.get<UserInfo>('/me').then(r => r.data),
}
