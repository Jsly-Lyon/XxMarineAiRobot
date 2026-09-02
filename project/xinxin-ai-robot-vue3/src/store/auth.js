import { ref, computed } from 'vue'

const TOKEN_KEY = 'hanhai_token'
const USER_KEY = 'hanhai_user'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    return null
  }
}

/** 登录 token（后续请求放入请求头 Authorization: Bearer <token>） */
export const token = ref(localStorage.getItem(TOKEN_KEY) || '')

/** 登录用户信息（本地冗余存储，展示用：username / nickname / avatar） */
export const userInfo = ref(readUser())

/** 是否已登录 */
export const isLoggedIn = computed(() => !!token.value)

// ============ 登录/注册弹窗全局状态（任意组件可打开） ============
/** 弹窗是否可见 */
export const authDialogVisible = ref(false)
/** 初始面板：login | register */
export const authDialogMode = ref('login')

/** 打开登录/注册弹窗 */
export function openAuthDialog(mode = 'login') {
  authDialogMode.value = mode
  authDialogVisible.value = true
}

/** 关闭登录/注册弹窗 */
export function closeAuthDialog() {
  authDialogVisible.value = false
}

/** 登录成功后写入 token 与用户信息 */
export function setAuth({ token: newToken, user }) {
  token.value = newToken
  userInfo.value = user || null
  try {
    localStorage.setItem(TOKEN_KEY, newToken)
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  } catch (error) {
    console.error('保存登录态失败:', error)
  }
}

/** 更新用户信息（如登录后从 /auth/info 拉取昵称/头像后回写） */
export function updateAuthUser(user) {
  userInfo.value = user || null
  try {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  } catch (error) {
    console.error('更新用户信息失败:', error)
  }
}

/** 退出登录，清空本地登录态 */
export function clearAuth() {
  token.value = ''
  userInfo.value = null
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch (error) {
    console.error('清除登录态失败:', error)
  }
}
