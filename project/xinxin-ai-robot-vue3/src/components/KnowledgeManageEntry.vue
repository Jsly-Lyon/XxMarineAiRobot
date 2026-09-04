<template>
  <a-tooltip title="文件管理" placement="left">
    <button
      class="fixed right-16 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white/80 text-gray-600 shadow-sm backdrop-blur transition-all hover:border-[#4d6bfe]/40 hover:text-[#4d6bfe] active:scale-95 dark:border-gray-700 dark:bg-gray-800/80 dark:text-gray-300 dark:hover:text-[#6d8bff]"
      aria-label="问答文件管理"
      @click="handleOpen">
      <SvgIcon name="settings" customCss="h-5 w-5" />
    </button>
  </a-tooltip>
</template>

<script setup>
import SvgIcon from '@/components/SvgIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useKnowledgeManageStore } from '@/stores/knowledgeManage'

const auth = useAuthStore()
const knowledge = useKnowledgeManageStore()

// 打开文件管理抽屉（未登录先弹登录框）
const handleOpen = () => {
  if (!auth.isLoggedIn) {
    auth.openAuthDialog('login')
    return
  }
  knowledge.open()
}
</script>
