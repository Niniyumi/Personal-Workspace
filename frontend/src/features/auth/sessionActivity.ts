const STORAGE_KEY = 'personal-agent.last-activity.v1'

export const SESSION_IDLE_MS = 15 * 60 * 1000

export const sessionActivity = {
  lastAt(): number | null {
    localStorage.removeItem(STORAGE_KEY)
    const value = sessionStorage.getItem(STORAGE_KEY)
    if (value === null) return null
    const timestamp = Number(value)
    return Number.isFinite(timestamp) ? timestamp : null
  },

  touch(): void {
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.setItem(STORAGE_KEY, String(Date.now()))
  },

  isInactive(): boolean {
    const lastAt = this.lastAt()
    return lastAt !== null && Date.now() - lastAt >= SESSION_IDLE_MS
  },

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(STORAGE_KEY)
  },
}
