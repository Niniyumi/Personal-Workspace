import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '../features/auth/authApi'
import ForgotPasswordView from './ForgotPasswordView.vue'

const push = vi.hoisted(() => vi.fn())
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('../features/auth/authApi', () => ({
  authApi: {
    requestPasswordReset: vi.fn(),
    confirmPasswordReset: vi.fn(),
  },
}))

describe('ForgotPasswordView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('requests a code and then resets the password', async () => {
    const wrapper = mount(ForgotPasswordView, {
      global: {
        stubs: {
          AuthShell: { template: '<main><slot /></main>' },
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    await wrapper.get('input[type="email"]').setValue('nini@example.com')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.requestPasswordReset).toHaveBeenCalledWith('nini@example.com')
    await wrapper.get('input[name="code"]').setValue('123456')
    await wrapper.get('input[name="newPassword"]').setValue('NewPassword8!')
    await wrapper.get('input[name="confirmPassword"]').setValue('NewPassword8!')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.confirmPasswordReset).toHaveBeenCalledWith({
      email: 'nini@example.com', code: '123456', newPassword: 'NewPassword8!',
    })
    expect(push).toHaveBeenCalledWith({ name: 'login', query: { reset: '1' } })
  })

  it('shows an actionable message when the reset email cannot be sent', async () => {
    vi.mocked(authApi.requestPasswordReset).mockRejectedValue({
      isAxiosError: true,
      response: { data: { code: 'VERIFICATION_MAIL_SEND_FAILED' } },
    })
    const wrapper = mount(ForgotPasswordView, {
      global: {
        stubs: {
          AuthShell: { template: '<main><slot /></main>' },
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    await wrapper.get('input[type="email"]').setValue('nini@example.com')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text())
      .toBe('验证码发送失败，请稍后重试；仍失败请联系管理员')
  })
})
