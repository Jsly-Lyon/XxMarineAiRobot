<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="relative flex flex-1 items-center justify-center overflow-hidden px-4">
        <!-- 装饰光晕 -->
        <div aria-hidden="true"
          class="pointer-events-none absolute -top-20 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full opacity-70 blur-3xl"
          style="background: radial-gradient(circle, rgba(77,107,254,.16), transparent 65%)">
        </div>

        <div class="relative w-full max-w-3xl">
          <div class="mb-12 text-center">
            <!-- 品牌 Logo 卡片 -->
            <div
              class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#4d6bfe] to-[#22d3ee] text-white shadow-lg shadow-[#4d6bfe]/25">
              <SvgIcon name="ai-robot-logo" customCss="h-9 w-9 text-white" />
            </div>

            <h2 class="text-3xl font-bold tracking-tight text-gray-900 md:text-4xl dark:text-gray-100">
              我是
              <span class="bg-gradient-to-r from-[#4d6bfe] via-[#3b82f6] to-[#0ea5e9] bg-clip-text text-transparent">
                瀚海知问
              </span>
              ，你的海洋科研 AI 助手
            </h2>
            <p class="mx-auto mt-4 max-w-xl text-[15px] leading-7 text-gray-500 dark:text-gray-400">
              我帮你高效检索海洋公开知识、整合并复用课题研究资料，请把你的问题交给我吧~
            </p>
          </div>

          <!-- 聊天输入框 -->
          <ChatInputBox
            v-model="userMessage"
            :loading="isSending"
            @send-message="sendMessage"
          />
        </div>
      </div>
    </template>
  </Layout>
</template>


<script setup>
import Layout from '@/layouts/Layout.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { newChat } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

// 用户输入的消息
const userMessage = ref('')
// 是否正在创建会话
const isSending = ref(false)
const router = useRouter()

// 发送消息：新建会话后跳转到对应聊天页，首句交给聊天页自动发送
const sendMessage = async ({ message: content } = {}) => {
  const text = (content ?? userMessage.value ?? '').trim()
  if (!text || isSending.value) return

  // 未登录时先弹登录框
  if (!auth.isLoggedIn) {
    auth.openAuthDialog('login')
    return
  }

  isSending.value = true
  try {
    const res = await newChat(text)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '新建对话失败')
    }

    const uuid = body.data?.uuid
    if (!uuid) {
      throw new Error('新建对话未返回会话 ID')
    }

    // 通过路由 history.state 隐式传递首条消息（不经过 URL，规避长度限制），
    // 聊天页挂载后会用首页所选模型/联网状态自动发送
    router.push({ path: `/chat/${uuid}`, state: { firstMessage: text } })
  } catch (error) {
    console.error('新建对话失败:', error)
    message.error(error?.message || '新建对话失败，请稍后重试')
    // 发送失败时把内容还回输入框，方便重试
    userMessage.value = text
  } finally {
    isSending.value = false
  }
}
</script>
