import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PreviewView from './PreviewView.vue'

describe('PreviewView', () => {
  it('shows the static agent workspace without backend data', () => {
    const wrapper = mount(PreviewView)

    expect(wrapper.get('h1').text()).toContain('今天想整理些什么')
    expect(wrapper.text()).toContain('对话式周报')
    expect(wrapper.text()).toContain('课程笔记')
    expect(wrapper.findAll('button').some((button) => button.text().includes('上传文档'))).toBe(true)
  })
})
