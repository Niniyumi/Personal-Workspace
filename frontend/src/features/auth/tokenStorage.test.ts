import { beforeEach, describe, expect, it } from 'vitest'
import { tokenStorage } from './tokenStorage'

describe('tokenStorage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('keeps login tokens for page reloads but not in persistent storage', () => {
    const tokens = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    }

    tokenStorage.write(tokens)

    expect(tokenStorage.read()).toEqual(tokens)
    expect(sessionStorage.length).toBe(1)
    expect(localStorage.length).toBe(0)
  })

  it('clears malformed stored data instead of throwing', () => {
    sessionStorage.setItem('personal-agent.auth.v1', '{invalid json')

    expect(tokenStorage.read()).toBeNull()
    expect(sessionStorage.getItem('personal-agent.auth.v1')).toBeNull()
  })

  it('requires login after the browser session ends', () => {
    tokenStorage.write({ accessToken: 'access', refreshToken: 'refresh', expiresInSeconds: 900 })

    sessionStorage.clear()

    expect(tokenStorage.read()).toBeNull()
  })

  it('does not restore an old persistent login from a previous version', () => {
    localStorage.setItem('personal-agent.auth.v1', JSON.stringify({
      accessToken: 'old-access', refreshToken: 'old-refresh', expiresInSeconds: 900,
    }))

    expect(tokenStorage.read()).toBeNull()
    expect(localStorage.getItem('personal-agent.auth.v1')).toBeNull()
  })

  it('clears the saved session', () => {
    tokenStorage.write({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    })

    tokenStorage.clear()

    expect(tokenStorage.read()).toBeNull()
  })
})
