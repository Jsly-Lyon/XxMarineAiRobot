<template>
  <div class="flex h-screen flex-col overflow-hidden">
    <!-- 聊天记录滚动区域 -->
    <div ref="chatContainerRef" class="chat-scrollbar flex-1 min-h-0 overflow-y-auto">
      <div class="mx-auto flex h-full w-full max-w-3xl flex-col px-5 py-6 md:px-6">
        <!-- 遍历聊天记录 -->
        <template v-for="(chat, index) in chatList" :key="index">
          <!-- 用户提问消息（靠右） -->
          <div v-if="chat.role === 'user'" class="mb-5 flex justify-end">
            <div
              class="max-w-[85%] whitespace-pre-wrap break-words rounded-2xl rounded-br-md bg-gradient-to-br from-[#4d6bfe] to-[#5b7cff] px-4 py-2.5 text-[15px] leading-6 text-white shadow-md shadow-[#4d6bfe]/20">
              <p>{{ chat.content }}</p>
            </div>
          </div>

          <!-- 大模型回复消息（靠左） -->
          <div v-else class="mb-6 flex items-start">
            <div class="mr-3 mt-0.5 flex-shrink-0">
              <div
                class="flex h-8 w-8 items-center justify-center rounded-full border border-gray-100 bg-white shadow-sm dark:border-[#3a4450] dark:bg-[#2a313c]">
                <SvgIcon name="deepseek-logo" customCss="h-5 w-5"></SvgIcon>
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
    <div class="shrink-0 border-t border-gray-200/60 bg-white px-5 pb-4 pt-3 md:px-6 dark:border-[#333a46] dark:bg-[#1f252e]">
      <div class="mx-auto w-full max-w-3xl">
        <ChatInputBox v-model="message" :loading="isWaiting" @send-message="sendMessage" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import LoadingDots from '@/components/LoadingDots.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { useAuthStore } from '@/stores/auth'
import { fetchEventSource } from '@microsoft/fetch-event-source'

const auth = useAuthStore()

// 输入的消息
const message = ref('')

// 聊天容器引用
const chatContainerRef = ref(null)

// 聊天记录：默认给一条欢迎语
const chatList = ref([
  {
    role: 'assistant',
    content: '你好呀！我是「瀚海知问」的 AI 智能客服，可以结合系统内置海洋知识与你的上传文档为你解答，欢迎提问 😁',
    loading: false,
  },
])

// 是否正在等待 AI 回复（用于禁用发送）
const isWaiting = computed(() => chatList.value.some((c) => c.role === 'assistant' && c.loading))

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const container = chatContainerRef.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

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

  try {
    // 构建请求体（后端 /customer-service/chat/completion：message 必填、chatId 可空）
    const requestBody = {
      message: userMessage,
      chatId: null,
    }

    const controller = new AbortController()
    const headers = { 'Content-Type': 'application/json' }
    if (auth.token) {
      headers.Authorization = `Bearer ${auth.token}`
    }

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
          updateLast()
          controller.abort()
          return
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
        controller.abort()
      },
    })
    updateLast()
    await scrollToBottom()
  } catch (error) {
    console.error('发送消息错误: ', error)
    const last = chatList.value[chatList.value.length - 1]
    last.content = '抱歉，请求出错了，请稍后重试。'
    last.loading = false
    await scrollToBottom()
  }
}
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
</style>
