import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it } from 'vitest'
import { createAuthApi } from './authApi'

const client = axios.create({ baseURL: '/api' })
const mock = new MockAdapter(client)
const api = createAuthApi(client)

afterEach(() => mock.reset())

describe('authApi', () => {
  it('sends the login contract and returns tokens', async () => {
    const tokens = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
    }
    mock.onPost('/auth/login', { login: 'nini', password: 'UnitTest7!' }).reply(200, tokens)

    await expect(api.login({ login: 'nini', password: 'UnitTest7!' })).resolves.toEqual(tokens)
  })

  it('sends the registration contract and returns the user', async () => {
    const input = {
      username: 'nini',
      email: 'nini@example.com',
      password: 'UnitTest7!',
      displayName: 'Nini',
    }
    const user = { id: 42, username: 'nini', email: 'nini@example.com', displayName: 'Nini' }
    mock.onPost('/auth/register', input).reply(201, user)

    await expect(api.register(input)).resolves.toEqual(user)
  })

  it('uses the bearer token to load the current user', async () => {
    const user = { id: 42, username: 'nini', email: 'nini@example.com', displayName: 'Nini' }
    mock.onGet('/users/me').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token')
      return [200, user]
    })

    await expect(api.currentUser('access-token')).resolves.toEqual(user)
  })

  it('refreshes and logs out with the refresh token', async () => {
    const tokens = {
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      expiresInSeconds: 900,
    }
    mock.onPost('/auth/refresh', { refreshToken: 'old-refresh-token' }).reply(200, tokens)
    mock.onPost('/auth/logout', { refreshToken: 'new-refresh-token' }).reply(204)

    await expect(api.refresh('old-refresh-token')).resolves.toEqual(tokens)
    await expect(api.logout('new-refresh-token')).resolves.toBeUndefined()
  })

  it('sends both password reset contracts', async () => {
    mock.onPost('/auth/password-reset/request', { email: 'nini@example.com' }).reply(204)
    mock.onPost('/auth/password-reset/confirm', {
      email: 'nini@example.com', code: '123456', newPassword: 'NewPassword8!',
    }).reply(204)

    await expect(api.requestPasswordReset('nini@example.com')).resolves.toBeUndefined()
    await expect(api.confirmPasswordReset({
      email: 'nini@example.com', code: '123456', newPassword: 'NewPassword8!',
    })).resolves.toBeUndefined()
  })
})
