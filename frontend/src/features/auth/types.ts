export interface AuthTokens {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface User {
  id: number
  username: string
  email: string
  displayName: string
}

export interface LoginInput {
  login: string
  password: string
}

export interface RegisterInput {
  username: string
  email: string
  password: string
  displayName: string
}

export interface ApiError {
  code: string
  message: string
  traceId: string
}
