export function logApiError(scope: string, cause: unknown) {
  const error = cause as {
    config?: { method?: string; url?: string }
    response?: { status?: number; data?: { code?: string; traceId?: string } }
  }
  console.error(`[${scope}] request failed`, {
    method: error.config?.method?.toUpperCase(),
    url: error.config?.url,
    status: error.response?.status,
    code: error.response?.data?.code,
    traceId: error.response?.data?.traceId,
  })
}
