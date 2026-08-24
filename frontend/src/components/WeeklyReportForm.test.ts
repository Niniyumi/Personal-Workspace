import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import WeeklyReportForm from './WeeklyReportForm.vue'

describe('WeeklyReportForm', () => {
  it('requires the core work before saving', async () => {
    const wrapper = mount(WeeklyReportForm)

    expect(wrapper.text()).toContain('本周核心工作')
    expect(wrapper.text()).toContain('遇到的问题')
    expect(wrapper.text()).toContain('下周工作计划')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('请填写本周核心工作')
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('appends imported content without replacing typed text', async () => {
    const wrapper = mount(WeeklyReportForm)
    await wrapper.get('[data-test="core-work"]').setValue('已经输入的内容')
    await wrapper.setProps({
      imported: {
        coreWork: '文档中的核心工作',
        problems: '文档中的问题',
        nextWeekPlan: '文档中的计划',
        sourceFileName: 'weekly.docx',
      },
    })

    expect((wrapper.get('[data-test="core-work"]').element as HTMLTextAreaElement).value)
      .toBe('已经输入的内容\n文档中的核心工作')
    expect((wrapper.get('[data-test="problems"]').element as HTMLTextAreaElement).value)
      .toBe('文档中的问题')
    expect((wrapper.get('[data-test="next-week-plan"]').element as HTMLTextAreaElement).value)
      .toBe('文档中的计划')
  })

  it('emits the three fields when saving', async () => {
    const wrapper = mount(WeeklyReportForm)
    await wrapper.get('[data-test="core-work"]').setValue('完成登录模块')
    await wrapper.get('[data-test="problems"]').setValue('接口联调耗时')
    await wrapper.get('[data-test="next-week-plan"]').setValue('开发周报')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')?.[0]?.[0]).toMatchObject({
      coreWork: '完成登录模块',
      problems: '接口联调耗时',
      nextWeekPlan: '开发周报',
    })
  })
})
