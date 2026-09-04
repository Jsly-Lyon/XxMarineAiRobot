<template>
  <a-drawer
    v-model:open="visible"
    title="智能客服"
    placement="right"
    :width="520"
    :closable="true"
    :body-style="{ padding: 0, height: '100%', overflow: 'hidden' }">
    <div class="flex h-full flex-col">
      <!-- 聊天记录滚动区域 -->
      <div ref="chatContainerRef" class="chat-scrollbar flex-1 min-h-0 overflow-y-auto">
        <div class="mx-auto flex h-full w-full max-w-xl flex-col px-4 py-4">
          <template v-for="(chat, index) in chatList" :key="index">
            <!-- 用户提问消息（靠右） -->
            <div v-if="chat.role === 'user'" class="mb-4 flex justify-end">
              <div
                class="max-w-[85%] whitespace-pre-wrap break-words rounded-2xl rounded-br-md bg-gradient-to-br from-[#4d6bfe] to-[#5b7cff] px-4 py-2 text-[15px] leading-6 text-white shadow-sm">
                <p>{{ chat.content }}</p>
              </div>
            </div>

            <!-- 大模型回复消息（靠左） -->
            <div v-else class="mb-5 flex items-start">
              <div class="mr-2.5 mt-0.5 flex-shrink-0">
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-full border border-gray-100 bg-white shadow-sm dark:border-[#3a4450] dark:bg-[#2a313c]">
                  <SvgIcon name="customer-service-logo" customCss="h-5 w-5" />
                </div>
              </div>
              <div class="min-w-0 flex-1">
                <div class="w-full">
                  <LoadingDots v-if="chat.loading" />
                  <StreamMarkdownRender v-if="chat.content" :content="chat.content" />
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 提问输入框 -->
      <div
        class="shrink-0 border-t border-gray-200/70 bg-white px-3 pb-3 pt-2 dark:border-[#333a46] dark:bg-[#1f252e]">
        <!-- 生成中可手动停止 -->
        <div v-if="isWaiting" class="mb-2 flex justify-center">
          <button
            class="inline-flex cursor-pointer items-center gap-1 rounded-full border border-gray-300 px-3 py-1 text-xs text-gray-600 transition-colors hover:bg-gray-100 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-[#2a313c]"
            @click="stopGeneration">
            <StopOutlined />
            停止生成
          </button>
        </div>
        <ChatInputBox v-model="message" :loading="isWaiting" :show-tools="false" @send-message="sendMessage" />
      </div>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { StopOutlined } from '@ant-design/icons-vue'
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import LoadingDots from '@/components/LoadingDots.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { useCustomerServiceStore } from '@/stores/customerService'
import { useAuthStore } from '@/stores/auth'
import { fetchEventSource } from '@microsoft/fetch-event-source'

const auth = useAuthStore()
const drawer = useCustomerServiceStore()

// 显隐（v-model 到全局状态）
const visible = computed({
  get: () => drawer.visible,
  set: (val) => {
    drawer.visible = val
  },
})

// 输入的消息
const message = ref('')

// 聊天容器引用
const chatContainerRef = ref(null)

// 聊天记录：默认一条欢迎语
const chatList = ref([
  {
    role: 'assistant',
    content: '你好呀！我是「瀚海知问」的 AI 智能客服，可以结合系统内置海洋知识与你的上传文档为你解答，欢迎提问 😁',
    loading: false,
  },
])

// 是否正在等待 AI 回复（用于禁用发送）
const isWaiting = computed(() => chatList.value.some((c) => c.role === 'assistant' && c.loading))

// 当前流式请求的取消控制器（供"停止生成"中止）
const activeController = ref(null)
// 是否为用户手动停止（停止后不覆盖已收到的内容）
let manualStop = false

// 兜底计时器句柄与清理（组件级，stopGeneration 与 sendMessage 共用）
let firstByteTimer = null
let streamTimer = null
const clearTimers = () => {
  if (firstByteTimer) clearTimeout(firstByteTimer)
  if (streamTimer) clearTimeout(streamTimer)
  firstByteTimer = null
  streamTimer = null
}

// 手动停止本次生成
const stopGeneration = () => {
  if (!activeController.value) return
  manualStop = true
  clearTimers()
  const controller = activeController.value
  activeController.value = null
  controller.abort()

  const last = chatList.value[chatList.value.length - 1]
  if (last && last.role === 'assistant') {
    last.loading = false
    if (!last.content) {
      last.content = '已停止生成。'
    }
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const container = chatContainerRef.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

// 打开抽屉时滚到底部，让欢迎语可见
watch(visible, (val) => {
  if (val) {
    scrollToBottom()
  }
})

// 保底超时（毫秒）：首个回复未达 / 整段回复过长
const FIRST_BYTE_TIMEOUT = 30_000
const STREAM_TIMEOUT = 90_000

// 发送消息
const sendMessage = async (payload = {}) => {
  const content = String(payload?.message ?? message.value ?? '').trim()
  if (!content || isWaiting.value) return

  // 未登录先弹登录框
  if (!auth.isLoggedIn) {
    auth.openAuthDialog('login')
    return
  }

  // 将用户发送的消息添加到列表并清空输入框
  const userMessage = content
  chatList.value.push({ role: 'user', content: userMessage })
  message.value = ''

  // 添加占位回复
  chatList.value.push({ role: 'assistant', content: '', loading: true })
  await scrollToBottom()

  // 兜底超时原因（每次发送前重置；计时器/清理为组件级）
  let timeoutMessage = ''
  manualStop = false

  try {
    // 构建请求体（后端 /customer-service/chat/completion：message 必填、chatId 可空）
    const requestBody = { message: userMessage, chatId: null }

    const controller = new AbortController()
    activeController.value = controller
    const headers = { 'Content-Type': 'application/json' }
    if (auth.token) {
      headers.Authorization = `Bearer ${auth.token}`
    }

    // 两档保底：① 30s 内无任何回复即中断；② 整段回复超 90s 即中断
    const abortWith = (msg) => {
      timeoutMessage = msg
      controller.abort()
    }

    let gotFirst = false
    firstByteTimer = setTimeout(() => abortWith('客服长时间未回复，请稍后重试。'), FIRST_BYTE_TIMEOUT)
    streamTimer = setTimeout(() => abortWith('本次回复时间过长，已中断，请重试。'), STREAM_TIMEOUT)

    // 收集流式回复并更新最后一条助手消息
    let responseText = ''
    const updateLast = () => {
      const last = chatList.value[chatList.value.length - 1]
      last.content = responseText
      last.loading = false
    }

    await fetchEventSource('/api/customer-service/chat/completion', {
      method: 'POST',
      signal: controller.signal,
      headers,
      body: JSON.stringify(requestBody),
      openWhenHidden: true, // 保持连接在页面隐藏时也不关闭
      onmessage(msg) {
        const data = (msg.data || '').trim()
        if (data === '[DONE]') {
          clearTimers()
          updateLast()
          activeController.value = null
          controller.abort()
          return
        }
        // 收到首个有效回复后，解除"无回复"保底
        if (!gotFirst) {
          gotFirst = true
          clearTimeout(firstByteTimer)
        }
        try {
          const parsed = JSON.parse(data)
          responseText += parsed?.v ?? ''
        } catch (error) {
          responseText += data
        }
        updateLast()
        scrollToBottom()
      },
      onerror(err) {
        console.error('智能客服 SSE 错误: ', err)
        clearTimers()
        activeController.value = null
        controller.abort()
      },
      // 服务端正常关闭：必须 abort，否则 fetch-event-source 会自动重连导致刷请求
      onclose() {
        clearTimers()
        activeController.value = null
        // 若全程未收到任何回复块（空流），给出兜底文案，避免"无任何输出"
        if (!gotFirst && !timeoutMessage) {
          responseText = '抱歉，客服暂时没有回复，请稍后重试。'
        }
        controller.abort()
      },
    })

    clearTimers()
    activeController.value = null
    updateLast()
    await scrollToBottom()
  } catch (error) {
    console.error('发送消息错误: ', error)
    clearTimers()
    activeController.value = null
    const last = chatList.value[chatList.value.length - 1]
    if (last && last.role === 'assistant') {
      // 用户手动停止：保留已收到的内容，不覆盖为报错
      if (!manualStop) {
        last.content = timeoutMessage || '抱歉，请求出错了，请稍后重试。'
      }
      last.loading = false
    }
    await scrollToBottom()
  }
}

// 卸载时中止进行中的流与计时器
onBeforeUnmount(() => {
  if (activeController.value) {
    activeController.value.abort()
  }
  activeController.value = null
  clearTimers()
})
</script>

<style scoped>
.chat-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: #d8dee8 transparent;
}
.chat-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.chat-scrollbar::-webkit-scrollbar-thumb {
  background-color: #d8dee8;
  border-radius: 999px;
}

/* 修复 antd 按钮内图标与文字不在同一行/不对齐（Tailwind reset 影响 svg 行内布局） */
:deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
}
:deep(.ant-btn .anticon),
:deep(.ant-btn-icon) {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}
:deep(.ant-btn .anticon svg) {
  vertical-align: middle;
}
</style>
