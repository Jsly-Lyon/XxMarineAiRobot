import { ref } from 'vue'

const THEME_KEY = 'hanhai_theme'

function readInitial() {
  try {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored) return stored === 'dark'
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  } catch (error) {
    return false
  }
}

/** 是否暗色模式（全局响应式） */
export const isDark = ref(readInitial())

/** 把当前主题同步到 <html> 的 class */
export function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
}

/** 应用初始主题（index.html 已预置，这里再兜底一次） */
export function initTheme() {
  applyTheme()
}

/** 切换主题并持久化 */
export function toggleTheme() {
  isDark.value = !isDark.value
  applyTheme()
  try {
    localStorage.setItem(THEME_KEY, isDark.value ? 'dark' : 'light')
  } catch (error) {
    console.error('保存主题失败:', error)
  }
}
