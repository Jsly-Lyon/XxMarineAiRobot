import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

// 旧版手动持久化的 localStorage key，仅用于首启迁移老登录态
const LEGACY_TOKEN_KEY = 'hanhai_token'
const LEGACY_USER_KEY = 'hanhai_user'

function readLegacyUser() {
  try {
    const raw = localStorage.getItem(LEGACY_USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    return null
  }
}

/**
 * 认证全局状态：token / 用户信息 / 登录注册弹窗。
 * 由 pinia-plugin-persistedstate 持久化（仅 token、userInfo），
 * token/userInfo 以外的 UI 状态（弹窗开合等）不持久化。
 */
export const useAuthStore = defineStore(
  'auth',
  () => {
    // token（旧 key 有值则迁移沿用，避免升级后需重新登录一次）
    const token = ref(localStorage.getItem(LEGACY_TOKEN_KEY) || '')

    // 当前登录用户信息（展示用：username / nickname / avatar）
    const userInfo = ref(readLegacyUser())

    // 是否已登录
    const isLoggedIn = computed(() => !!token.value)

    // 登录/注册弹窗
    const authDialogVisible = ref(false)
    const authDialogMode = ref('login')

    // 打开弹窗
    function openAuthDialog(mode = 'login') {
      authDialogMode.value = mode
      authDialogVisible.value = true
    }

    // 关闭弹窗
    function closeAuthDialog() {
      authDialogVisible.value = false
    }

    // 登录成功后写入 token 与用户信息
    function setAuth({ token: newToken, user }) {
      token.value = newToken
      userInfo.value = user || null
    }

    // 更新用户信息（如登录后从 /auth/info 拉取昵称/头像后回写）
    function updateAuthUser(user) {
      userInfo.value = user || null
    }

    // 退出登录，清空本地登录态（含旧 key 冗余）
    function clearAuth() {
      token.value = ''
      userInfo.value = null
      try {
        localStorage.removeItem(LEGACY_TOKEN_KEY)
        localStorage.removeItem(LEGACY_USER_KEY)
      } catch (error) {
        console.error('清除旧登录态失败:', error)
      }
    }

    return {
      token,
      userInfo,
      isLoggedIn,
      authDialogVisible,
      authDialogMode,
      openAuthDialog,
      closeAuthDialog,
      setAuth,
      updateAuthUser,
      clearAuth,
    }
  },
  {
    persist: {
      key: 'hanhai_auth',
      // 只持久化 token 与用户信息；弹窗开关等 UI 状态不落盘
      pick: ['token', 'userInfo'],
    },
  }
)
