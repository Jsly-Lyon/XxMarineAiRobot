<template>
  <button
    class="fixed right-4 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white/80 text-gray-600 shadow-sm backdrop-blur transition-all hover:border-[#4d6bfe]/40 hover:text-[#4d6bfe] active:scale-95 dark:border-gray-700 dark:bg-gray-800/80 dark:text-gray-300 dark:hover:text-[#6d8bff]"
    :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    aria-label="切换主题"
    @click="toggleTheme">
    <!-- 暗色：月亮；亮色：太阳 -->
    <svg v-if="isDark" viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8"
      stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </svg>
    <svg v-else viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8"
      stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  </button>
</template>

<script setup>
import { isDark, toggleTheme } from '@/utils/theme'
</script>
