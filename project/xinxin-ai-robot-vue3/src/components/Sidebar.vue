<template>
  <!-- 左边栏 -->
  <div
    :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    class="w-64 bg-[#f9fbff] border-r border-gray-200 fixed left-0 top-0 h-full transition-transform duration-300 ease-in-out z-10 overflow-y-auto"
  >
    <div class="p-0 h-full flex flex-col">
      <!-- Logo -->
      <div class="flex items-center justify-center p-4 cursor-pointer" @click="jumpToIndexPage">
        <SvgIcon name="ai-robot-logo" customCss="w-8 h-8 text-gray-700 mr-3" />
        <span class="text-2xl font-bold font-sans tracking-wide text-gray-800">瀚海知问 AI</span>
      </div>

      <!-- 新对话按钮：未登录时先登录 -->
      <button
        @click="handleStartNewChat"
        class="mx-auto mb-[34px] my-2 px-6 py-2 text-white rounded-xl transition-colors new-chat-btn w-fit cursor-pointer"
      >
        <SvgIcon name="new-chat" customCss="w-6 h-6 mr-1.5 inline text-[#4d6bfe]" />
        开启新对话
      </button>

      <!-- 历史对话区域（仅登录后显示；未登录不展示，也不请求后端） -->
      <template v-if="isLoggedIn">
      <div class="my-4 px-2 overflow-y-auto overflow-x-hidden flex-1">
        <div class="text-xs px-3 py-1 text-gray-500">历史对话</div>

        <div class="space-y-1">
          <div
            v-for="historyChat in historyChats"
            :key="historyChat.uuid"
            class="relative px-3 py-1 rounded-xl hover:bg-[rgb(239,246,255)] cursor-pointer transition-colors flex items-center justify-between group"
            @click="openChat(historyChat.uuid)"
            @mouseenter="showButton = historyChat.uuid"
            @mouseleave="showButton = null"
          >
            <!-- Tooltip 展示完整标题 -->
            <a-tooltip placement="top" :mouse-enter-delay="0.5">
              <template #title>{{ historyChat.summary }}</template>
              <p class="text-[14px] text-gray-800 overflow-hidden whitespace-nowrap pr-6">
                {{ historyChat.summary }}
              </p>
            </a-tooltip>

            <!-- 下拉菜单（重命名/删除） -->
            <a-dropdown :trigger="['click']" @visibleChange="(visible) => { if (visible) showButton = historyChat.uuid }">
              <template #overlay>
                <a-menu>
                  <a-menu-item key="rename">
                    <EditOutlined /> 重命名
                  </a-menu-item>
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>

              <!-- 操作按钮：hover 显示；@click.stop 避免触发整行跳转 -->
              <button
                class="z-10 rounded-lg outline-none justify-center items-center bg-white w-6 h-6 flex absolute right-2 top-1/2 transform -translate-y-1/2 transition-all duration-300 hover:bg-gray-50"
                :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }"
                @click.stop
              >
                <EllipsisOutlined class="w-4 h-4 text-gray-500" />
              </button>
            </a-dropdown>
          </div>

          <!-- 空态 -->
          <div v-if="!historyChats.length"
            class="flex flex-col items-center justify-center gap-2 px-3 py-6 text-xs text-gray-400">
            <SvgIcon name="chat-empty" customCss="h-8 w-8 text-gray-300" />
            <span>暂时无历史对话</span>
          </div>
        </div>
      </div>
      </template>

      <!-- 未登录：占位，把底部登录入口推到最底 -->
      <div v-else class="flex-1"></div>

      <!-- 底部用户区 -->
      <div class="border-t border-gray-200 px-3 py-3">
        <!-- 已登录：默认头像 + 用户信息 + 退出 -->
        <div v-if="isLoggedIn" class="flex items-center gap-2.5 rounded-xl px-2 py-1.5">
          <img v-if="userInfo?.avatar" :src="userInfo.avatar"
            class="h-9 w-9 flex-shrink-0 rounded-full object-cover" alt="avatar" />
          <div v-else class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-[#4d6bfe] text-white">
            <UserOutlined class="text-base" />
          </div>
          <div class="min-w-0 flex-1 text-left">
            <div class="truncate text-sm font-medium text-gray-800">{{ displayName }}</div>
            <div class="truncate text-xs text-gray-400">{{ userInfo?.username || '' }}</div>
          </div>
          <a-tooltip title="退出登录">
            <button
              class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
              @click="handleLogout">
              <LogoutOutlined class="text-base" />
            </button>
          </a-tooltip>
        </div>

        <!-- 未登录：显示登录入口 -->
        <button
          v-else
          class="flex w-full items-center justify-center gap-2 rounded-xl py-2 text-sm text-gray-600 transition-colors hover:bg-gray-100"
          @click="openAuthDialog('login')">
          <UserOutlined class="text-gray-400" />
          登录 / 注册
        </button>
      </div>
    </div>
  </div>

  <!-- 侧边栏切换按钮 -->
  <a-tooltip placement="bottom">
    <template #title>{{ sidebarOpen ? '收缩边栏' : '打开边栏' }}</template>
    <button
      :class="sidebarOpen ? 'left-64' : 'left-0'"
      @click="toggleSidebar"
      class="fixed top-4 z-20 bg-white border border-gray-200 rounded-r-lg p-2 transition-all duration-300"
    >
      <SvgIcon
        :name="sidebarOpen ? 'sidebar-open' : 'sidebar-close'"
        :customCss="sidebarOpen ? 'w-6 h-6 text-gray-400' : 'w-7 h-7 text-gray-400'"
      />
    </button>
  </a-tooltip>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { message } from 'ant-design-vue'
import {
  EllipsisOutlined, EditOutlined, DeleteOutlined, UserOutlined, LogoutOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { findChatHistoryPageList } from '@/api/chat'
import { isLoggedIn, userInfo, openAuthDialog, clearAuth } from '@/store/auth'
import { logout as logoutApi } from '@/api/auth'

const router = useRouter()

const props = defineProps({
  sidebarOpen: { type: Boolean, required: true },
})

const emit = defineEmits(['toggle-sidebar'])

const historyChats = ref([])
const showButton = ref(null)

const toggleSidebar = () => {
  emit('toggle-sidebar')
}

const jumpToIndexPage = () => {
  router.push('/')
}

// 展示名：昵称优先，其次用户名
const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')

// 开启新对话：未登录先弹登录框
const handleStartNewChat = () => {
  if (!isLoggedIn.value) {
    openAuthDialog('login')
    return
  }
  jumpToIndexPage()
}

// 退出登录：清空登录态并回到首页，避免停留在上一个账号的会话页
const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    // token 失效等场景后端可能拒绝，忽略并照常清空本地登录态
  }
  clearAuth()
  message.success('已退出登录')
  router.push('/')
}

// 点击历史会话，跳转到对应聊天页
const openChat = (uuid) => {
  if (!uuid) return
  router.push(`/chat/${uuid}`)
}

// 加载后端历史会话列表（仅登录后）
const loadHistoryChats = async () => {
  if (!isLoggedIn.value) {
    historyChats.value = []
    return
  }
  try {
    const res = await findChatHistoryPageList({ current: 1, size: 100 })
    const body = res?.data
    if (!body || body.success === false) return

    historyChats.value = (body.data ?? []).map((item) => ({
      uuid: item.uuid,
      summary: item.summary,
    }))
  } catch (error) {
    console.error('加载历史会话失败:', error)
    historyChats.value = []
  }
}

// 登录成功后加载历史，退出登录后清空
watch(isLoggedIn, (logged) => {
  if (logged) {
    loadHistoryChats()
  } else {
    historyChats.value = []
  }
})

onMounted(() => {
  loadHistoryChats()
})
</script>

<style scoped>
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}
.new-chat-btn {
  background-color: rgb(219 234 254);
  color: #4d6bfe;
}
.new-chat-btn:hover {
  background-color: #c6dcf8;
}
</style>
