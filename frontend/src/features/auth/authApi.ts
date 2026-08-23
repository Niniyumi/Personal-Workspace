import axios, { type AxiosInstance } from 'axios'
import type { AuthTokens, LoginInput, RegisterInput, User } from './types'

export function createAuthApi(client: AxiosInstance) {
  return {
    async register(input: RegisterInput): Promise<User> {
      const response = await client.post<User>('/auth/register', input)
      return response.data
    },

    async login(input: LoginInput): Promise<AuthTokens> {
      const response = await client.post<AuthTokens>('/auth/login', input)
      return response.data
    },

    async currentUser(accessToken: string): Promise<User> {
      const response = await client.get<User>('/users/me', {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      return response.data
    },

    async refresh(refreshToken: string): Promise<AuthTokens> {
      const response = await client.post<AuthTokens>('/auth/refresh', { refreshToken })
      return response.data
    },

    async logout(refreshToken: string): Promise<void> {
      await client.post('/auth/logout', { refreshToken })
    },
  }
}

export const authApi = createAuthApi(axios.create({ baseURL: '/api' }))
