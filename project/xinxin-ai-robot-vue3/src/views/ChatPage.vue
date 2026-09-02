<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="flex flex-col flex-1 min-h-0 min-w-0">

        <!-- 聊天记录滚动区域：内容限定在屏幕中间一列 -->
        <div ref="chatContainerRef" class="chat-scrollbar flex-1 min-h-0 overflow-y-auto">
          <div class="mx-auto flex h-full w-full max-w-3xl flex-col px-5 pt-6 pb-4 md:px-6">
            <!-- 加载历史中 -->
            <div v-if="isHistoryLoading" class="flex flex-1 items-center justify-center text-sm text-gray-400">
              历史消息加载中…
            </div>

            <!-- 无消息时的空态 -->
            <div v-else-if="!chatList.length"
              class="flex flex-1 flex-col items-center justify-center gap-3 text-gray-400">
              <SvgIcon name="ai-robot-logo" customCss="w-10 h-10 text-gray-300" />
              <p class="text-sm">开始和瀚海知问对话吧</p>
            </div>

            <!-- 消息列表 -->
            <template v-else>
              <template v-for="(chat, index) in chatList" :key="index">
                <!-- 用户提问消息（靠右） -->
                <div v-if="chat.role === 'user'" class="mb-7 flex justify-end">
                  <div class="question-container">
                    <p>{{ chat.content }}</p>
                  </div>
                </div>

                <!-- 大模型回复消息（靠左） -->
                <div v-else class="mb-8 flex items-start">
                  <div class="mr-3 mt-1 flex-shrink-0">
                    <div
                      class="flex h-8 w-8 items-center justify-center rounded-full border border-gray-200 bg-white shadow-sm">
                      <SvgIcon name="deepseek-logo" customCss="h-5 w-5"></SvgIcon>
                    </div>
                  </div>
                  <div class="max-w-[92%] min-w-0">
                    <!-- 添加 answer-container 类名以应用样式 -->
                    <div class="answer-container">
                      <StreamMarkdownRender :content="chat.content" />
                    </div>
                    <span v-if="chat.timestamp" class="mt-1 block pl-1 text-xs text-gray-400">{{ chat.timestamp }}</span>
                  </div>
                </div>
              </template>
            </template>
          </div>
        </div>

        <!-- 提问输入框：ChatGPT 风格，与消息同列居中，不占满整屏 -->
        <div class="shrink-0 border-t border-gray-100 bg-white/70 px-5 pt-2 pb-3 md:px-6">
          <div class="mx-auto w-full max-w-3xl">
            <ChatInputBox
              v-model="message"
              :loading="isLoading || isHistoryLoading"
              @send-message="handleSend"
            />
          </div>
        </div>
      </div>
    </template>
  </Layout>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import Layout from '@/layouts/Layout.vue';
import SvgIcon from '@/components/SvgIcon.vue';
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue';
import ChatInputBox from '@/components/ChatInputBox.vue';
import { findChatHistoryMessageList, streamChatCompletion } from '@/api/chat';
import { consumePendingFirstMessage } from '@/utils/pendingFirstMessage';
import { isLoggedIn, openAuthDialog } from '@/store/auth';

// 默认模型名（ChatInputBox 模型列表默认项）
const DEFAULT_MODEL = 'deepseek-v3';
// 进入会话时一次性加载的历史条数
const HISTORY_PAGE_SIZE = 200;

const route = useRoute();
// 会话 UUID，来自路由 /chat/:chatId
const chatId = computed(() => String(route.params.chatId || '').trim());

const message = ref('');
const chatList = ref([]);
const chatContainerRef = ref(null);
// 是否正在等待 AI 流式回复
const isLoading = ref(false);
// 是否正在加载历史消息
const isHistoryLoading = ref(false);

// 当前流式请求的取消控制器
let abortController = null;

// 获取当前时间 HH:mm
const getCurrentTime = () => {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
};

// 将后端 LocalDateTime 格式化为 HH:mm（兼容 ISO / 带空格两种格式）
const formatDisplayTime = (time) => {
  if (!time) return '';
  const str = String(time).replace('T', ' ');
  const matched = str.match(/\d{2}:\d{2}/);
  return matched ? matched[0] : '';
};

// 解析单个 SSE 数据块，取 JSON 中的 v 字段；非 JSON 时按纯文本兼容
const parseStreamContent = (rawData) => {
  if (!rawData) {
    return '';
  }

  try {
    const data = JSON.parse(rawData);
    return data?.v ?? '';
  } catch (error) {
    console.warn('SSE 数据不是有效 JSON，已按纯文本兼容处理:', rawData);
    return rawData;
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const container = chatContainerRef.value;
    if (!container) return;

    requestAnimationFrame(() => {
      container.scrollTop = container.scrollHeight;
    });
  });
};

// 中断当前流
const abortStream = () => {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
  isLoading.value = false;
};

// 流结束统一收尾（错误时兜底一条提示）
const finalizeStream = (hasError = false) => {
  isLoading.value = false;
  const lastMsg = chatList.value[chatList.value.length - 1];
  if (hasError && lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
    lastMsg.content = '抱歉，请求出错了，请稍后重试。';
  }
};

// 加载该会话的历史消息
const loadHistory = async () => {
  // 未登录不请求会话数据（接口已强制鉴权），避免进入页面触发 401
  if (!isLoggedIn.value) {
    chatList.value = [];
    isHistoryLoading.value = false;
    return;
  }

  const id = chatId.value;
  if (!id) {
    chatList.value = [];
    isHistoryLoading.value = false;
    return;
  }

  isHistoryLoading.value = true;
  try {
    const res = await findChatHistoryMessageList({
      chatId: id,
      current: 1,
      size: HISTORY_PAGE_SIZE,
    });

    const body = res?.data;
    if (body && body.success === false) {
      throw new Error(body.message || '加载历史消息失败');
    }

    const list = body?.data ?? [];
    chatList.value = list.map((m) => ({
      role: m.role === 'user' ? 'user' : 'assistant',
      content: m.content ?? '',
      timestamp: formatDisplayTime(m.createTime),
    }));
  } catch (error) {
    console.error('加载历史消息失败:', error);
    chatList.value = [];
  } finally {
    isHistoryLoading.value = false;
    scrollToBottom();
  }
};

// 发送消息
const handleSend = (payload = {}) => {
  const content = String(payload?.message ?? message.value ?? '').trim();
  if (!content || isLoading.value || isHistoryLoading.value) return;

  // 未登录时先弹登录框
  if (!isLoggedIn.value) {
    openAuthDialog('login');
    return;
  }

  const modelName = payload?.modelName || DEFAULT_MODEL;
  const networkSearch = !!payload?.networkSearch;

  // 追加用户消息
  chatList.value.push({ role: 'user', content, timestamp: getCurrentTime() });
  message.value = '';

  // 追加 AI 占位消息
  chatList.value.push({ role: 'assistant', content: '', timestamp: getCurrentTime() });
  isLoading.value = true;
  scrollToBottom();

  // 发起流式请求（POST + SSE）
  abortController = new AbortController();
  streamChatCompletion(
    { message: content, chatId: chatId.value, modelName, networkSearch },
    {
      signal: abortController.signal,
      onData: (rawData) => {
        const chunk = parseStreamContent(rawData);
        if (!chunk) return;

        const lastMsg = chatList.value[chatList.value.length - 1];
        if (lastMsg && lastMsg.role === 'assistant') {
          lastMsg.content += chunk;
        }
        scrollToBottom();
      },
      onDone: () => {
        abortController = null;
        finalizeStream(false);
      },
      onError: (error) => {
        console.error('流式对话错误: ', error);
        abortController = null;
        const lastMsg = chatList.value[chatList.value.length - 1];
        if (lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
          lastMsg.content = error?.message || '抱歉，请求出错了，请稍后重试。';
        }
        finalizeStream(true);
      },
    }
  );
};

// 若本会话是“首页新建”并暂存了首句，则打开后自动发送一次
const tryConsumePendingInitial = () => {
  const id = chatId.value;
  if (!id) return;

  // 隐式读取（sessionStorage）并立即清除，避免 URL query 长度限制
  const payload = consumePendingFirstMessage(id);
  if (!payload) return;

  handleSend(payload);
};

// 切换会话时：中断旧流、清空列表、重新加载历史，再消费待发送的首句
watch(chatId, async () => {
  abortStream();
  chatList.value = [];
  await loadHistory();
  tryConsumePendingInitial();
}, { immediate: true });

onBeforeUnmount(() => {
  abortStream();
});
</script>

<style scoped>
/* 淡化聊天区域滚动条，降低视觉干扰 */
.chat-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: #d8dee8 transparent;
}

.chat-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.chat-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.chat-scrollbar::-webkit-scrollbar-thumb {
  background-color: #d8dee8;
  border-radius: 999px;
}

.chat-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: #c7cfdd;
}

/* 用户提问气泡 */
.question-container {
  font-size: 14px;
  line-height: 24px;
  color: #1f2328;
  padding: 8px 15px;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: #f4f7fb;
  border-radius: 14px;
  max-width: calc(100% - 48px);
}

/* 大模型回复气泡 */
.answer-container {
  color: #111827;
  padding: 2px 0 0;
  background-color: transparent;
  word-break: break-word;
  white-space: normal;
}
</style>
