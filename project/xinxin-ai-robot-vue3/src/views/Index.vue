<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="flex flex-1 items-center justify-center relative">
        <div class="max-w-3xl w-full">
          <div class="text-center mb-10">
            <div class="flex items-center justify-center mb-3">
              <SvgIcon name="ai-robot-logo" customCss="w-10 h-10 text-gray-700 mr-3" />
              <h2 class="text-2xl text-gray-800">我是瀚海知问，你的海洋科研 AI 助手</h2>
            </div>
            <p class="text-gray-500">我帮你高效检索海洋公开知识、整合并复用课题研究资料，请把你的问题交给我吧~</p>
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
import { savePendingFirstMessage } from '@/utils/pendingFirstMessage'
import { isLoggedIn, openAuthDialog } from '@/store/auth'

// 用户输入的消息
const userMessage = ref('')
// 是否正在创建会话
const isSending = ref(false)
const router = useRouter()

// 发送消息：新建会话后跳转到对应聊天页，首句交给聊天页自动发送
const sendMessage = async ({ message: content, modelName, networkSearch } = {}) => {
  const text = (content ?? userMessage.value ?? '').trim()
  if (!text || isSending.value) return

  // 未登录时先弹登录框
  if (!isLoggedIn.value) {
    openAuthDialog('login')
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

    // 将首条消息隐式暂存（sessionStorage），聊天页打开后自动发送；
    // 不走 URL query，避免超长消息触发长度限制
    savePendingFirstMessage(uuid, { message: text, modelName, networkSearch })

    router.push(`/chat/${uuid}`)
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
