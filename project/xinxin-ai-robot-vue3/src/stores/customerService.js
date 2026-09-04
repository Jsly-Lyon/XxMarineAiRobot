import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 智能客服右侧抽屉的开合状态（全局可用，任意组件可打开/关闭）。
 */
export const useCustomerServiceStore = defineStore('customerService', () => {
  /** 抽屉是否可见 */
  const visible = ref(false)

  function open() {
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  return { visible, open, close }
})
