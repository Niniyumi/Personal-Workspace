import { useAuthStore } from '../features/auth/authStore'

export async function authenticatedRequest<T>(operation: (accessToken: string) => Promise<T>): Promise<T> {
  const auth = useAuthStore()
  const accessToken = auth.tokens?.accessToken
  if (!accessToken) {
    auth.expireSession()
    throw new Error('登录状态已失效')
  }

  try {
    return await operation(accessToken)
  } catch (cause) {
    if (!isUnauthorized(cause)) throw cause
  }

  const refreshedToken = await auth.refreshAccessToken()
  try {
    return await operation(refreshedToken)
  } catch (cause) {
    if (isUnauthorized(cause)) auth.expireSession()
    throw cause
  }
}

function isUnauthorized(cause: unknown): boolean {
  return typeof cause === 'object' && cause !== null
    && 'response' in cause
    && (cause as { response?: { status?: number } }).response?.status === 401
}
