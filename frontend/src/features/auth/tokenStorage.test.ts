import { describe, expect, it } from 'vitest'
import { tokenStorage } from './tokenStorage'

describe('tokenStorage', () => {
  it('round-trips access and refresh tokens under one key', () => {
    const tokens = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    }

    tokenStorage.write(tokens)

    expect(tokenStorage.read()).toEqual(tokens)
    expect(localStorage.length).toBe(1)
  })

  it('clears malformed stored data instead of throwing', () => {
    localStorage.setItem('personal-agent.auth.v1', '{invalid json')

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
