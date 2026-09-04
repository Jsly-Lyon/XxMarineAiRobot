<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="relative flex min-h-0 min-w-0 flex-1 flex-col">

        <!-- 背景层：为对话区增加纵深与层次感（不随消息滚动） -->
        <div aria-hidden="true" class="pointer-events-none absolute inset-0 overflow-hidden">
          <div class="absolute -top-28 left-[10%] h-96 w-96 rounded-full blur-3xl"
            style="background: radial-gradient(circle, rgba(77,107,254,.11), transparent 65%)"></div>
          <div class="absolute bottom-24 right-[4%] h-80 w-80 rounded-full blur-3xl"
            style="background: radial-gradient(circle, rgba(56,189,248,.13), transparent 65%)"></div>
          <div class="absolute inset-0 bg-gradient-to-b from-white/45 via-white/5 to-[#e9effb]/70 dark:from-transparent dark:via-transparent dark:to-[#0e1726]/80"></div>
        </div>

        <!-- 聊天记录滚动区域：内容限定在屏幕中间一列 -->
        <div ref="chatContainerRef" class="chat-scrollbar relative flex-1 min-h-0 overflow-y-auto"
          @scroll="handleScroll">
          <div class="mx-auto flex h-full w-full max-w-3xl flex-col px-5 pt-6 pb-4 md:px-6">
            <!-- 加载历史中 -->
            <div v-if="isHistoryLoading" class="flex flex-1 items-center justify-center text-sm text-gray-400">
              历史消息加载中…
            </div>

            <!-- 无消息时的空态 -->
            <div v-else-if="!chatList.length"
              class="flex flex-1 flex-col items-center justify-center gap-4">
              <div
                class="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#eef3ff] to-[#e0f7ff] shadow-sm dark:from-[#232b36] dark:to-[#1c232e]">
                <SvgIcon name="ai-robot-logo" customCss="h-9 w-9 text-[#4d6bfe] dark:text-[#8fa6ff]" />
              </div>
              <div class="text-center">
                <p class="text-[15px] font-medium text-gray-700 dark:text-gray-200">开始和瀚海知问对话吧</p>
                <p class="mt-1 text-xs text-gray-400 dark:text-gray-500">向它提出海洋科研问题，体验 RAG 检索式智能问答</p>
              </div>
            </div>

            <!-- 消息列表 -->
            <template v-else>
              <template v-for="(chat, index) in chatList" :key="index">
                <!-- 用户提问消息（靠右，品牌色气泡） -->
                <div v-if="chat.role === 'user'" class="mb-6 flex justify-end">
                  <div
                    class="max-w-[85%] whitespace-pre-wrap break-words rounded-2xl rounded-br-md bg-gradient-to-br from-[#4d6bfe] to-[#5b7cff] px-4 py-2.5 text-[15px] leading-6 text-white shadow-md shadow-[#4d6bfe]/20">
                    <p>{{ chat.content }}</p>
                  </div>
                </div>

                <!-- 大模型回复消息（靠左） -->
                <div v-else class="mb-8 flex items-start">
                  <div class="mr-3 mt-0.5 flex-shrink-0">
                    <div
                      class="flex h-8 w-8 items-center justify-center rounded-full border border-gray-100 bg-white shadow-sm ring-1 ring-black/[0.02] dark:border-[#3a4450] dark:bg-[#2a313c]">
                      <SvgIcon name="deepseek-logo" customCss="h-5 w-5"></SvgIcon>
                    </div>
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="answer-container w-full">
                      <!-- loading 为 true 时展示三点加载动画 -->
                      <LoadingDots v-if="chat.loading" />

                      <!-- 推理过程展示（可折叠） -->
                      <div v-if="chat.reasoning" class="mb-5 text-gray-500 dark:text-gray-400">
                        <div
                          class="mb-1 flex cursor-pointer select-none items-center gap-0.5"
                          @click="toggleReasoning(chat)">
                          <span class="text-[13px] font-medium">深度思考</span>
                          <SvgIcon
                            name="down-arrow"
                            :customCss="`inline h-4 w-4 transition-transform duration-200 ${chat.collapsedReasoning ? 'rotate-180' : ''}`" />
                        </div>
                        <StreamMarkdownRender
                          v-if="!chat.collapsedReasoning"
                          customCss="px-2 border-l-2 border-gray-200 text-gray-500! dark:border-gray-600"
                          :content="chat.reasoning" />
                      </div>

                      <!-- 正式回答 -->
                      <StreamMarkdownRender v-if="chat.content" :content="chat.content" />
                    </div>
                    <span v-if="chat.timestamp" class="mt-1.5 block pl-1 text-xs text-gray-400">{{ chat.timestamp }}</span>
                  </div>
                </div>
              </template>
            </template>
          </div>
        </div>

        <!-- 提问输入框：ChatGPT 风格，与消息同列居中，不占满整屏（不透明底，避免内容透视） -->
        <div class="shrink-0 border-t border-gray-200/60 bg-white px-5 pt-3 pb-4 md:px-6 dark:border-[#333a46] dark:bg-[#1f252e]">
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
import LoadingDots from '@/components/LoadingDots.vue';
import { findChatMessagePageList, streamChatCompletion } from '@/api/chat';
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chatStore';

const auth = useAuthStore();
const chatStore = useChatStore();

// 默认模型名（ChatInputBox 模型列表默认项）
const DEFAULT_MODEL = 'deepseek-v3';
// 进入会话时一次性加载的历史条数
const HISTORY_PAGE_SIZE = 200;
// 首页跳转时通过路由 history.state 带来的初始消息（仅本实例首次消费一次）
const initialFirstMessage = (window.history.state?.firstMessage || '').trim();

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
// 历史分页：当前已加载到的页码、是否还有更早的数据、是否正在加载上一页
const currentPage = ref(1);
const hasMoreHistory = ref(true);
const isLoadingMoreHistory = ref(false);

// 后端消息 VO -> 前端聊天项
const mapHistoryMessage = (m) => ({
  role: m.role === 'user' ? 'user' : 'assistant',
  content: m.content ?? '',
  reasoning: m.reasoning ?? '',
  collapsedReasoning: false,
  timestamp: formatDisplayTime(m.createTime),
});

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

// 解析单个 SSE 数据块：取 JSON 中的 v（正式回答）与 reasoning（推理过程）字段；非 JSON 时按纯文本兼容
const parseStreamPayload = (rawData) => {
  if (!rawData) {
    return { v: '', reasoning: '' };
  }

  try {
    const data = JSON.parse(rawData);
    return { v: data?.v ?? '', reasoning: data?.reasoning ?? '' };
  } catch (error) {
    console.warn('SSE 数据不是有效 JSON，已按纯文本兼容处理:', rawData);
    return { v: rawData, reasoning: '' };
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

// 流结束统一收尾（隐藏加载动画；错误时兜底一条提示）
const finalizeStream = (hasError = false) => {
  isLoading.value = false;
  const lastMsg = chatList.value[chatList.value.length - 1];
  if (lastMsg && lastMsg.role === 'assistant') {
    lastMsg.loading = false;
  }
  if (hasError && lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
    lastMsg.content = '抱歉，请求出错了，请稍后重试。';
  }
};

// 切换某条消息推理内容的折叠状态
const toggleReasoning = (chat) => {
  chat.collapsedReasoning = !chat.collapsedReasoning;
};

// 统一分页请求：返回 PageResponse 的 body，失败则抛出业务错误
const fetchHistoryPage = async (page) => {
  const res = await findChatMessagePageList({
    chatId: chatId.value,
    current: page,
    size: HISTORY_PAGE_SIZE,
  });
  const body = res?.data;
  if (!body || body.success === false) {
    throw new Error(body?.message || '加载历史消息失败');
  }
  return body;
};

// 加载第一页（最新一屏）历史：刷新后默认滚动到对话末尾
const loadHistory = async () => {
  // 未登录不请求会话数据（接口已强制鉴权），避免进入页面触发 401
  if (!auth.isLoggedIn || !chatId.value) {
    chatList.value = [];
    isHistoryLoading.value = false;
    return;
  }

  isHistoryLoading.value = true;
  try {
    const body = await fetchHistoryPage(1);
    chatList.value = (body.data ?? []).map(mapHistoryMessage);
    // 还有比第一页更早的数据才可继续上翻
    hasMoreHistory.value = (body.pages || 0) > 1;
  } catch (error) {
    console.error('加载历史消息失败:', error);
    chatList.value = [];
  } finally {
    isHistoryLoading.value = false;
    scrollToBottom();
  }
};

// 滚动到接近顶部时，加载更早一页并追加到列表顶部（保持阅读位置不跳动）
const loadOlderHistory = async () => {
  if (!hasMoreHistory.value || isLoadingMoreHistory.value || isHistoryLoading.value) return;

  const el = chatContainerRef.value;
  const prevScrollTop = el?.scrollTop ?? 0;
  const prevScrollHeight = el?.scrollHeight ?? 0;

  isLoadingMoreHistory.value = true;
  const nextPage = currentPage.value + 1;
  try {
    const body = await fetchHistoryPage(nextPage);
    const older = (body.data ?? []).map(mapHistoryMessage);
    if (!older.length) {
      hasMoreHistory.value = false;
      return;
    }

    chatList.value = [...older, ...chatList.value];
    currentPage.value = nextPage;
    hasMoreHistory.value = (body.pages || 0) > nextPage;

    // 顶部新增了内容，向下补偿滚动距离，避免视图跳动
    nextTick(() => {
      const container = chatContainerRef.value;
      if (!container) return;
      container.scrollTop = prevScrollTop + (container.scrollHeight - prevScrollHeight);
    });
  } catch (error) {
    console.error('加载更多历史消息失败:', error);
  } finally {
    isLoadingMoreHistory.value = false;
  }
};

// 聊天容器滚动监听：接近顶部时触发上翻加载（有更多数据且未在加载中）
const handleScroll = () => {
  const el = chatContainerRef.value;
  if (!el) return;
  if (el.scrollTop < 50 && hasMoreHistory.value && !isLoadingMoreHistory.value && !isHistoryLoading.value) {
    loadOlderHistory();
  }
};

// 发送消息
const handleSend = (payload = {}) => {
  const content = String(payload?.message ?? message.value ?? '').trim();
  if (!content || isLoading.value || isHistoryLoading.value) return;

  // 未登录时先弹登录框
  if (!auth.isLoggedIn) {
    auth.openAuthDialog('login');
    return;
  }

  const modelName = payload?.modelName || DEFAULT_MODEL;
  const networkSearch = !!payload?.networkSearch;

  // 追加用户消息
  chatList.value.push({ role: 'user', content, timestamp: getCurrentTime() });
  message.value = '';

  // 追加 AI 占位消息（loading=true：展示三点加载动画，首个回复块到达后关闭）
  chatList.value.push({
    role: 'assistant',
    content: '',
    reasoning: '',
    collapsedReasoning: false,
    loading: true,
    timestamp: getCurrentTime(),
  });
  isLoading.value = true;
  scrollToBottom();

  // 发起流式请求（POST + SSE）
  abortController = new AbortController();
  streamChatCompletion(
    { message: content, chatId: chatId.value, modelName, networkSearch },
    {
      signal: abortController.signal,
      onData: (rawData) => {
        const { v: textChunk, reasoning: reasoningChunk } = parseStreamPayload(rawData);
        if (!textChunk && !reasoningChunk) return;

        const lastMsg = chatList.value[chatList.value.length - 1];
        if (lastMsg && lastMsg.role === 'assistant') {
          // 收到首个回复块后隐藏加载动画
          if (lastMsg.loading) {
            lastMsg.loading = false;
          }
          // 推理过程增量（后端按帧下发增量，这里逐帧累积）
          if (reasoningChunk) {
            lastMsg.reasoning += reasoningChunk;
          }
          // 正式回答增量
          if (textChunk) {
            lastMsg.content += textChunk;
          }
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

// 清除 history.state 中携带的首条消息，防止刷新页面后重复发送
const clearFirstMessageState = () => {
  if (!window.history.state?.firstMessage) return;
  const newState = { ...window.history.state };
  delete newState.firstMessage;
  window.history.replaceState(newState, document.title);
};

// 首页跳转带来的首条消息：仅首次自动发送一次，带上首页已选模型/联网状态
let autoSentFirst = false;
const trySendFirstMessage = () => {
  if (autoSentFirst || !initialFirstMessage) return;
  autoSentFirst = true;
  handleSend({
    message: initialFirstMessage,
    modelName: chatStore.selectedModel?.name || DEFAULT_MODEL,
    networkSearch: chatStore.isNetworkSearchSelected,
  });
  // 发送后即清除，刷新不再重复发送
  clearFirstMessageState();
};

// 切换会话时：中断旧流、重置分页、清空列表、重新加载第一页；首次进入则自动发送首页首条
watch(chatId, async () => {
  abortStream();
  currentPage.value = 1;
  hasMoreHistory.value = true;
  isLoadingMoreHistory.value = false;
  chatList.value = [];
  await loadHistory();
  trySendFirstMessage();
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
