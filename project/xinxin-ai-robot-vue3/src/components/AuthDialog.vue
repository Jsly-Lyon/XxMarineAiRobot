<template>
  <a-modal v-model:open="visible" :footer="null" :closable="true" :maskClosable="true" centered width="420"
    :destroy-on-close="false">
    <div class="auth-dialog">
      <!-- 标题与品牌 -->
      <div class="mb-6 text-center">
        <div
          class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-[#4d6bfe] to-[#22d3ee] text-white shadow-lg shadow-[#4d6bfe]/25">
          <SvgIcon name="ai-robot-logo" customCss="h-6 w-6 text-white" />
        </div>
        <h3 class="text-xl font-bold tracking-tight text-gray-900 dark:text-gray-100">欢迎来到瀚海知问</h3>
        <p class="mt-1 text-xs text-gray-400 dark:text-gray-500">登录后开启你的海洋科研智能问答之旅</p>
      </div>

      <!-- 登录/注册面板切换 -->
      <div class="mb-6 grid grid-cols-2 gap-1 rounded-2xl bg-[#f1f3f9] p-1 dark:bg-gray-800">
        <button
          class="rounded-xl py-2 text-sm font-medium transition-all"
          :class="activeForm === 'login'
            ? 'bg-white text-[#3b5bef] shadow-sm dark:bg-gray-700 dark:text-[#8fa6ff]'
            : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
          @click="switchForm('login')">
          登 录
        </button>
        <button
          class="rounded-xl py-2 text-sm font-medium transition-all"
          :class="activeForm === 'register'
            ? 'bg-white text-[#3b5bef] shadow-sm dark:bg-gray-700 dark:text-[#8fa6ff]'
            : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
          @click="switchForm('register')">
          注 册
        </button>
      </div>

      <!-- 登录表单 -->
      <form v-if="activeForm === 'login'" class="flex flex-col gap-3" @submit.prevent="handleLogin">
        <a-input v-model:value="loginForm.username" size="large" placeholder="用户名" allow-clear autocomplete="username"
          class="w-full" />
        <a-input-password v-model:value="loginForm.password" size="large" placeholder="密码" autocomplete="current-password"
          class="w-full" @keyup.enter="handleLogin" />
        <a-button type="primary" size="large" html-type="submit" block :loading="submitting"
          class="!mt-1 auth-submit-btn">
          登 录
        </a-button>
        <p class="mt-1 text-center text-xs text-gray-400">
          还没有账号？
          <a class="cursor-pointer text-[#4d6bfe]" @click="switchForm('register')">立即注册</a>
        </p>
      </form>

      <!-- 注册表单 -->
      <form v-else class="flex flex-col gap-3" @submit.prevent="handleRegister">
        <a-input v-model:value="registerForm.username" size="large" placeholder="用户名" allow-clear autocomplete="username"
          class="w-full" />
        <a-input v-model:value="registerForm.nickname" size="large" placeholder="昵称（可选，缺省用用户名）" allow-clear
          class="w-full" />
        <a-input-password v-model:value="registerForm.password" size="large" placeholder="密码（6~64 位）"
          autocomplete="new-password" class="w-full" />
        <a-input-password v-model:value="registerForm.confirmPassword" size="large" placeholder="确认密码"
          autocomplete="new-password" class="w-full" @keyup.enter="handleRegister" />
        <a-button type="primary" size="large" html-type="submit" block :loading="submitting"
          class="!mt-1 auth-submit-btn">
          注册并登录
        </a-button>
        <p class="mt-1 text-center text-xs text-gray-400">
          已有账号？
          <a class="cursor-pointer text-[#4d6bfe]" @click="switchForm('login')">去登录</a>
        </p>
      </form>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import SvgIcon from '@/components/SvgIcon.vue'

const auth = useAuthStore()

// 弹窗显隐（v-model 到全局状态）
const visible = computed({
  get: () => auth.authDialogVisible,
  set: (val) => {
    auth.authDialogVisible = val
  },
})

const activeForm = ref('login')
const submitting = ref(false)
const router = useRouter()

// 登录/注册成功后：若当前还停留在某会话页，跳回首页，
// 避免新账号继续停留在上一个账号的 /chat/:id 上触发“此对话不存在”
const redirectHomeIfOnChat = () => {
  if (router.currentRoute.value.path.startsWith('/chat/')) {
    router.replace('/')
  }
}

// 打开弹窗时，按触发方指定的面板初始化
watch(
  () => auth.authDialogVisible,
  (val) => {
    if (val) {
      activeForm.value = auth.authDialogMode || 'login'
    }
  }
)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const switchForm = (form) => {
  activeForm.value = form
  auth.authDialogMode = form
}

// 读取服务端 Response 里的业务错误信息
const readBodyMessage = (res, fallback) => {
  const body = res?.data
  if (body && body.success === false) return body.message || fallback
  return fallback
}

// 登录后从后端拉取用户信息（后端读 Redis 缓存），回写本地用于展示昵称/头像
const refreshProfile = async (fallbackUsername) => {
  try {
    const res = await getUserInfo()
    const body = res?.data
    if (!body || body.success === false || !body.data) return

    const info = body.data
    auth.updateAuthUser({
      id: info.id,
      username: info.username || fallbackUsername,
      nickname: info.nickname || info.username || fallbackUsername,
      avatar: info.avatar || null,
    })
  } catch (error) {
    // 拉取失败不阻塞已登录状态，下次登录或刷新时再同步
  }
}

// 登录
const handleLogin = async () => {
  const username = loginForm.username.trim()
  if (!username || !loginForm.password) {
    message.warning('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    const res = await loginApi({ username, password: loginForm.password })
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '登录失败')
    }
    const newToken = body.data?.token
    if (!newToken) throw new Error('未获取到登录凭证')

    // 先以用户名本地快速展示，再异步从 /auth/info（Redis 缓存）拉取昵称/头像回写
    auth.setAuth({ token: newToken, user: { username } })
    message.success('登录成功')
    auth.closeAuthDialog()
    redirectHomeIfOnChat()
    refreshProfile(username)
  } catch (error) {
    message.error(error?.message || readBodyMessage(error, '登录失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

// 注册成功后自动登录
const handleRegister = async () => {
  const username = registerForm.username.trim()
  const nickname = registerForm.nickname.trim() || username
  const { password, confirmPassword } = registerForm

  if (!username) {
    message.warning('请输入用户名')
    return
  }
  if (!password || password.length < 6 || password.length > 64) {
    message.warning('密码长度需在 6~64 位之间')
    return
  }
  if (password !== confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    const regRes = await registerApi({ username, nickname, password })
    const regBody = regRes?.data
    if (!regBody || regBody.success === false) {
      throw new Error(regBody?.message || '注册失败')
    }

    // 注册成功自动登录
    const loginRes = await loginApi({ username, password })
    const loginBody = loginRes?.data
    if (!loginBody || loginBody.success === false) {
      throw new Error(loginBody?.message || '注册成功，自动登录失败，请手动登录')
    }
    const newToken = loginBody.data?.token
    if (!newToken) throw new Error('未获取到登录凭证')

    auth.setAuth({ token: newToken, user: { username, nickname } })
    message.success('注册成功，已自动登录')
    auth.closeAuthDialog()
    redirectHomeIfOnChat()
    refreshProfile(username)
  } catch (error) {
    message.error(error?.message || '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-submit-btn {
  background-image: linear-gradient(135deg, #4d6bfe 0%, #3b82f6 100%);
  border-color: transparent;
  box-shadow: 0 6px 18px rgba(77, 107, 254, 0.28);
  transition: opacity 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
}
.auth-submit-btn:hover,
.auth-submit-btn:focus {
  background-image: linear-gradient(135deg, #405ff0 0%, #2f73f2 100%);
  border-color: transparent;
  box-shadow: 0 8px 22px rgba(77, 107, 254, 0.35);
}
.auth-submit-btn:active {
  transform: scale(0.99);
}
</style>
