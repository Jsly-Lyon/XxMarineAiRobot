<template>
  <!-- 左边栏 -->
  <div
    :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    class="fixed left-0 top-0 z-10 h-full w-64 overflow-y-auto border-r border-gray-200/70 bg-[#fbfcff]/90 backdrop-blur-xl shadow-[2px_0_24px_rgba(30,41,59,0.04)] transition-transform duration-300 ease-in-out dark:border-[#333a46] dark:bg-[#1e242d]/95 dark:shadow-none"
  >
    <div class="flex h-full flex-col">
      <!-- Logo -->
      <div class="flex cursor-pointer items-center justify-center py-5" @click="jumpToIndexPage">
        <SvgIcon name="ai-robot-logo" customCss="mr-3 h-8 w-8 flex-shrink-0 text-[#4d6bfe]" />
        <span class="whitespace-nowrap font-sans text-2xl font-bold tracking-wide text-gray-800 dark:text-gray-100">瀚海知问 AI</span>
      </div>

      <!-- 新对话按钮：未登录时先登录 -->
      <button
        @click="handleStartNewChat"
        class="mx-4 flex cursor-pointer items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-[#4d6bfe] to-[#3b82f6] py-2.5 text-sm font-medium text-white shadow-md shadow-[#4d6bfe]/25 transition-all hover:brightness-105 active:scale-[0.99]"
      >
        <SvgIcon name="new-chat" customCss="inline h-5 w-5 text-white" />
        开启新对话
      </button>

      <!-- 下载记录入口（预留文档上传/下载） -->
      <button
        @click="openDownloadHistory"
        class="mx-4 mt-2 flex cursor-pointer items-center justify-center gap-1.5 rounded-xl border border-gray-200/80 py-2 text-sm font-medium text-gray-500 transition-colors hover:border-[#4d6bfe]/40 hover:text-[#4d6bfe] dark:border-gray-700 dark:text-gray-400 dark:hover:text-[#8fa6ff]">
        <DownloadOutlined class="text-base" />
        下载记录
      </button>

      <!-- 历史对话区域（仅登录后显示；未登录不展示，也不请求后端） -->
      <template v-if="isLoggedIn">
      <div class="my-5 flex-1 overflow-y-auto overflow-x-hidden px-2.5" @scroll="handleHistoryScroll">
        <div class="px-3 pb-1.5 pt-1 text-[11px] font-semibold uppercase tracking-wider text-gray-400">历史对话</div>

        <div class="space-y-0.5">
          <div
            v-for="historyChat in historyChats"
            :key="historyChat.uuid"
            class="group relative flex cursor-pointer items-center justify-between rounded-lg px-3 py-2 transition-colors hover:bg-[#eef4ff] dark:hover:bg-[#2a313c]"
            @click="openChat(historyChat.uuid)"
            @mouseenter="showButton = historyChat.uuid"
            @mouseleave="showButton = null"
          >
            <!-- Tooltip 展示完整标题 -->
            <a-tooltip placement="top" :mouse-enter-delay="0.5">
              <template #title>{{ historyChat.summary }}</template>
              <p class="overflow-hidden whitespace-nowrap pr-7 text-[14px] text-gray-700 dark:text-gray-200">
                {{ historyChat.summary }}
              </p>
            </a-tooltip>

            <!-- 下拉菜单（重命名/删除） -->
            <a-dropdown :trigger="['click']" @visibleChange="(visible) => { if (visible) showButton = historyChat.uuid }">
              <template #overlay>
                <a-menu @click="handleMenuClick(historyChat.uuid, historyChat.id, historyChat.summary, $event)">
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
                class="absolute right-1.5 top-1/2 z-10 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-lg bg-white outline-none transition-all duration-200 hover:bg-gray-50 dark:bg-[#2a313c] dark:hover:bg-gray-600"
                :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }"
                @click.stop
              >
                <EllipsisOutlined class="h-4 w-4 text-gray-500" />
              </button>
            </a-dropdown>
          </div>

          <!-- 空态 -->
          <div v-if="!historyChats.length"
            class="flex flex-col items-center justify-center gap-2 px-3 py-8 text-xs text-gray-400">
            <SvgIcon name="chat-empty" customCss="h-10 w-10 text-gray-300" />
            <span>暂时无历史对话</span>
          </div>
        </div>
      </div>
      </template>

      <!-- 未登录：占位，把底部登录入口推到最底 -->
      <div v-else class="flex-1"></div>

      <!-- 底部用户区 -->
      <div class="border-t border-gray-200/70 px-3 py-3 dark:border-[#333a46]">
        <div class="rounded-xl px-2 py-1.5">
          <!-- 已登录：默认头像 + 用户信息 + 退出 -->
          <div v-if="isLoggedIn" class="flex items-center gap-2.5">
            <img v-if="userInfo?.avatar" :src="userInfo.avatar"
              class="h-9 w-9 flex-shrink-0 rounded-full object-cover ring-2 ring-[#e8edff]" alt="avatar" />
            <div v-else
              class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#4d6bfe] to-[#22d3ee] text-white">
              <UserOutlined class="text-base" />
            </div>
            <div class="min-w-0 flex-1 text-left">
              <div class="truncate text-sm font-semibold text-gray-800 dark:text-gray-100">{{ displayName }}</div>
              <div class="truncate text-xs text-gray-400">{{ userInfo?.username || '' }}</div>
            </div>
            <a-tooltip title="退出登录">
              <button
                class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-700 dark:hover:text-gray-200"
                @click="handleLogout">
                <LogoutOutlined class="text-base" />
              </button>
            </a-tooltip>
          </div>

          <!-- 未登录：显示登录入口 -->
          <button
            v-else
            class="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#4d6bfe] to-[#3b82f6] py-2.5 text-sm font-medium text-white shadow-sm transition-all hover:brightness-105"
            @click="openAuthDialog('login')">
            <UserOutlined />
            登录 / 注册
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 侧边栏切换按钮 -->
  <a-tooltip placement="bottom">
    <template #title>{{ sidebarOpen ? '收缩边栏' : '打开边栏' }}</template>
    <button
      :class="sidebarOpen ? 'left-64' : 'left-0'"
      @click="toggleSidebar"
      class="fixed top-4 z-20 bg-white border border-gray-200 rounded-r-lg p-2 transition-all duration-300 dark:bg-[#262c36] dark:border-[#333a46]"
    >
      <SvgIcon
        :name="sidebarOpen ? 'sidebar-open' : 'sidebar-close'"
        :customCss="sidebarOpen ? 'w-6 h-6 text-gray-400' : 'w-7 h-7 text-gray-400'"
      />
    </button>
  </a-tooltip>

  <!-- 删除对话确认框 -->
  <a-modal
    v-model:open="deleteChatModelOpen"
    title="永久删除对话"
    width="400px"
    :centered="true"
    ok-text="确认"
    cancel-text="取消"
    :ok-button-props="{ danger: true }"
    :confirm-loading="deleteChatLoading"
    @ok="handleDeleteChatModelOk">
    <p>删除后，该对话及其全部消息将不可恢复。确认删除吗？</p>
  </a-modal>

  <!-- 重命名对话弹出框 -->
  <a-modal
    v-model:open="renameChatModelOpen"
    width="480px"
    :centered="true"
    title="重命名对话"
    ok-text="确认"
    cancel-text="取消"
    :confirm-loading="renameChatLoading"
    @ok="handleRenameChatModelOk">
    <a-form ref="formRef" :model="formState" :rules="renameRules" layout="vertical" autocomplete="off">
      <a-form-item label="对话摘要" name="summary">
        <a-input v-model:value="formState.summary" placeholder="请输入对话摘要" @pressEnter="handleRenameChatModelOk" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 下载记录弹窗 -->
  <DownloadHistoryDialog :open="downloadHistoryOpen" @update:open="downloadHistoryOpen = $event" />
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { message } from 'ant-design-vue'
import {
  EllipsisOutlined, EditOutlined, DeleteOutlined, UserOutlined, LogoutOutlined, DownloadOutlined,
} from '@ant-design/icons-vue'
import DownloadHistoryDialog from '@/components/DownloadHistoryDialog.vue'
import { useRouter } from 'vue-router'
import { findChatHistoryPageList, deleteChat, renameChat } from '@/api/chat'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { logout as logoutApi } from '@/api/auth'

const router = useRouter()

const props = defineProps({
  sidebarOpen: { type: Boolean, required: true },
})

const emit = defineEmits(['toggle-sidebar'])

const auth = useAuthStore()
const { isLoggedIn, userInfo } = storeToRefs(auth)
// 供模板/脚本统一调用的本地包装（避免把 store 实例暴露进模板各处）
const openAuthDialog = (mode = 'login') => auth.openAuthDialog(mode)
const clearAuth = () => auth.clearAuth()

const historyChats = ref([])
const showButton = ref(null)

// 删除对话确认框
const deleteChatModelOpen = ref(false)
// 待删除的对话 UUID
const deleteChatUUID = ref(null)
// 删除请求中（防止重复提交）
const deleteChatLoading = ref(false)

// 重命名对话弹窗
const renameChatModelOpen = ref(false)
// 重命名请求中（防止重复提交）
const renameChatLoading = ref(false)
// 表单引用与校验规则
const formRef = ref(null)
const renameRules = {
  summary: [{ required: true, message: '请输入对话摘要', trigger: ['change', 'blur'] }],
}
// 重命名表单数据
const formState = reactive({
  id: null, // 被重命名对话的 ID
  summary: '', // 摘要
})

// 下载记录弹窗（预留文档上传/下载）
const downloadHistoryOpen = ref(false)

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

// 查看下载记录：未登录先弹登录框
const openDownloadHistory = () => {
  if (!isLoggedIn.value) {
    openAuthDialog('login')
    return
  }
  downloadHistoryOpen.value = true
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

// 历史行下拉菜单点击：删除 / 重命名
const handleMenuClick = (uuid, id, summary, e) => {
  const key = e?.key
  if (key === 'delete') {
    deleteChatUUID.value = uuid
    deleteChatModelOpen.value = true
  } else if (key === 'rename') {
    // 打开重命名弹窗并回填当前摘要
    formState.id = id
    formState.summary = summary || ''
    renameChatModelOpen.value = true
  }
}

// 重命名确认：先做前端校验，通过后请求后端接口
const handleRenameChatModelOk = async () => {
  if (renameChatLoading.value) return

  try {
    // 校验不通过会 reject，由表单组件展示错误
    await formRef.value?.validate()
  } catch (error) {
    // 校验失败：无需额外处理，表单已高亮
    return
  }

  const summary = formState.summary.trim()
  if (!summary) return

  renameChatLoading.value = true
  try {
    const res = await renameChat(formState.id, summary)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '重命名失败，请稍后重试')
    }

    message.success('操作成功！')
    // 更新本地列表中的摘要
    const index = historyChats.value.findIndex((chat) => chat.id === formState.id)
    if (index !== -1) {
      historyChats.value[index].summary = summary
    }
    renameChatModelOpen.value = false
  } catch (error) {
    message.error(error?.message || '重命名失败，请稍后重试')
  } finally {
    renameChatLoading.value = false
  }
}

// 删除确认：请求后端删除，成功后移除列表项；若正停留在该会话则回首页
const handleDeleteChatModelOk = async () => {
  const uuid = deleteChatUUID.value
  if (!uuid || deleteChatLoading.value) return

  deleteChatLoading.value = true
  try {
    const res = await deleteChat(uuid)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '删除失败，请稍后重试')
    }

    message.success('删除成功')
    const idx = historyChats.value.findIndex((chat) => chat.uuid === uuid)
    if (idx !== -1) historyChats.value.splice(idx, 1)

    // 若当前就停留在这个会话，删除后回到首页
    if (router.currentRoute.value.path === `/chat/${uuid}`) {
      router.push('/')
    }
    deleteChatModelOpen.value = false
  } catch (error) {
    message.error(error?.message || '删除失败，请稍后重试')
  } finally {
    deleteChatLoading.value = false
  }
}

// ===== 历史会话分页（向下滚动加载下一页） =====
const HISTORY_LIST_PAGE_SIZE = 100
// 当前已加载到的页码
const historyPage = ref(1)
// 是否还有下一页
const hasMoreHistoryChats = ref(true)
// 是否正在加载下一页（防并发）
const isLoadingMoreHistoryChats = ref(false)

const mapHistoryChat = (item) => ({ id: item.id, uuid: item.uuid, summary: item.summary })

// 按当前页码请求，成功则把结果追加到列表末尾；返回是否成功
const fetchHistoryChats = async () => {
  if (!isLoggedIn.value) return false
  try {
    const res = await findChatHistoryPageList({ current: historyPage.value, size: HISTORY_LIST_PAGE_SIZE })
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '加载历史会话失败')
    }

    const list = (body.data ?? []).map(mapHistoryChat)
    historyChats.value = [...historyChats.value, ...list]
    // 总页数大于当前页才认为还有下一页
    hasMoreHistoryChats.value = (body.pages || 0) > historyPage.value
    return true
  } catch (error) {
    console.error('加载历史会话失败:', error)
    return false
  } finally {
    isLoadingMoreHistoryChats.value = false
  }
}

// 重置分页并加载第一页
const resetAndLoadHistory = async () => {
  historyPage.value = 1
  hasMoreHistoryChats.value = true
  isLoadingMoreHistoryChats.value = false
  historyChats.value = []
  await fetchHistoryChats()
}

// 滚动到底部附近（<=20px）时加载下一页
const handleHistoryScroll = (event) => {
  const el = event?.currentTarget
  if (!el) return
  const scrollPosition = el.scrollHeight - el.scrollTop - el.clientHeight
  if (scrollPosition <= 20 && hasMoreHistoryChats.value && !isLoadingMoreHistoryChats.value) {
    loadMoreHistoryChats()
  }
}

// 加载下一页历史会话（带并发与末页保护）
const loadMoreHistoryChats = async () => {
  if (!hasMoreHistoryChats.value || isLoadingMoreHistoryChats.value) return

  isLoadingMoreHistoryChats.value = true
  const nextPage = historyPage.value + 1
  const prevPage = historyPage.value
  historyPage.value = nextPage

  const ok = await fetchHistoryChats()
  if (!ok) {
    // 请求失败回退页码，便于下次重试
    historyPage.value = prevPage
  }
}

// 登录成功后加载第一页；退出登录后清空并复位分页
watch(isLoggedIn, (logged) => {
  if (logged) {
    resetAndLoadHistory()
  } else {
    historyChats.value = []
    historyPage.value = 1
    hasMoreHistoryChats.value = true
    isLoadingMoreHistoryChats.value = false
  }
})

onMounted(() => {
  resetAndLoadHistory()
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
