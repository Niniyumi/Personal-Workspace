import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from './HomeView.vue'
import LoginView from './LoginView.vue'
import RegisterView from './RegisterView.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  store: {
    status: 'anonymous',
    error: null as string | null,
    user: null as null | { id: number; username: string; email: string; displayName: string },
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('../features/auth/authStore', () => ({
  useAuthStore: () => mocks.store,
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: mocks.push }),
    useRoute: () => ({ query: {} }),
  }
})

const global = { stubs: { RouterLink: RouterLinkStub } }

beforeEach(() => {
  vi.clearAllMocks()
  mocks.store.status = 'anonymous'
  mocks.store.error = null
  mocks.store.user = null
  mocks.store.login = mocks.login
  mocks.store.register = mocks.register
  mocks.store.logout = mocks.logout
})

describe('LoginView', () => {
  it('submits username or email and password then navigates home', async () => {
    mocks.login.mockResolvedValue(undefined)
    const wrapper = mount(LoginView, { global })

    expect(wrapper.find('label[for="login"]').text()).toContain('用户名或邮箱')
    expect(wrapper.find('label[for="password"]').text()).toContain('密码')
    await wrapper.get('#login').setValue('nini@example.com')
    await wrapper.get('#password').setValue('UnitTest7!')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.login).toHaveBeenCalledWith({ login: 'nini@example.com', password: 'UnitTest7!' })
    expect(mocks.push).toHaveBeenCalledWith('/')
  })

  it('shows the store error and disables submit while loading', () => {
    mocks.store.error = '用户名、邮箱或密码不正确'
    mocks.store.status = 'loading'

    const wrapper = mount(LoginView, { global })

    expect(wrapper.get('[role="alert"]').text()).toContain('用户名、邮箱或密码不正确')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})

describe('RegisterView', () => {
  it('submits all registration fields then sends the user to login', async () => {
    mocks.register.mockResolvedValue({ id: 42 })
    const wrapper = mount(RegisterView, { global })

    await wrapper.get('#username').setValue('nini')
    await wrapper.get('#email').setValue('nini@example.com')
    await wrapper.get('#displayName').setValue('Nini')
    await wrapper.get('#password').setValue('UnitTest7!')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.register).toHaveBeenCalledWith({
      username: 'nini',
      email: 'nini@example.com',
      displayName: 'Nini',
      password: 'UnitTest7!',
    })
    expect(mocks.push).toHaveBeenCalledWith({ name: 'login', query: { registered: '1' } })
  })
})

describe('HomeView', () => {
  it('shows the current user and logs out to the login page', async () => {
    mocks.store.user = {
      id: 42,
      username: 'nini',
      email: 'nini@example.com',
      displayName: 'Nini',
    }
    mocks.logout.mockResolvedValue(undefined)
    const wrapper = mount(HomeView, { global })

    expect(wrapper.text()).toContain('Nini')
    expect(wrapper.text()).toContain('nini@example.com')
    await wrapper.get('[data-test="logout"]').trigger('click')
    await flushPromises()

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.push).toHaveBeenCalledWith({ name: 'login' })
  })
})
