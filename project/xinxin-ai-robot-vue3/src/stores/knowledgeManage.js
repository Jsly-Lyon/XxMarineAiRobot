import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 问答文件管理抽屉的开合状态（全局可用：右上角 settings 入口、客服抽屉入口都可打开）。
 */
export const useKnowledgeManageStore = defineStore('knowledgeManage', () => {
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
