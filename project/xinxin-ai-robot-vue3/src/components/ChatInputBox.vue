<template>
    <div class="flex w-full flex-col">
        <div
          class="flex flex-col rounded-3xl border border-gray-200 bg-white/95 px-4 py-3.5 shadow-[0_8px_28px_rgba(24,39,75,0.06)] backdrop-blur transition-shadow duration-200 focus-within:border-[#4d6bfe]/35 focus-within:shadow-[0_0_0_4px_rgba(77,107,254,0.08)] dark:border-[#3a4450] dark:bg-[#222933]/95 dark:shadow-none dark:focus-within:border-[#6d8bff]/60">
        <textarea placeholder="向瀚海知问提问，开启海洋科研探索"
            class="resize-none w-full bg-transparent text-sm leading-6 text-gray-800 outline-none placeholder:text-gray-400 min-h-[24px] dark:text-gray-100 dark:placeholder:text-gray-500"
            rows="1"
            v-model="userMessage"
            ref="textareaRef"
            @input="autoResize"
            @keydown.enter="handleEnterKey"></textarea>

         <!-- 下方容器 -->
        <div class="flex mt-3">
            <!-- 模型/联网工具行（showTools=false 时不展示） -->
            	<div v-if="showTools" class="flex gap-2 relative">
                <!-- 大模型下拉框 -->
                <div
                  class="flex cursor-pointer items-center justify-center rounded-xl border border-gray-200 px-2.5 py-1.5 transition-colors hover:border-[#4d6bfe]/30 hover:bg-[#f5f7ff] dark:border-gray-600 dark:hover:border-[#6d8bff]/50 dark:hover:bg-[#2a313c]"
                  ref="selectRef"
                  @click="toggleModelDropdown">
                    <SvgIcon :name="currSelectedModel.icon" customCss="mr-1.5 h-5 w-5" />
                    <span class="text-xs font-medium text-gray-800 dark:text-gray-100">{{ currSelectedModel.name }}</span>
                    <SvgIcon name="down-arrow" customCss="ml-1 h-4 w-4 text-gray-500 transition-transform duration-300"
                    :class="isModelDropdownOpen ? 'rotate-180' : ''" />
                </div>

                <!-- 下拉框菜单：向上展开（输入框常在屏幕底部，向下会被裁切） -->
                <div v-if="isModelDropdownOpen"
                  class="absolute bottom-full left-0 z-20 mb-2 w-56 overflow-hidden rounded-2xl border border-gray-100 bg-white p-1.5 shadow-xl shadow-gray-200/60 dark:border-[#3a4450] dark:bg-[#262d37] dark:shadow-none">
                    <div v-for="model in models" :key="model.id"
                    class="flex cursor-pointer items-center justify-between rounded-xl px-2.5 py-2 transition-colors hover:bg-[#f5f7ff] dark:hover:bg-gray-700/80"
                    @click="selectModel(model)">
                        <div class="flex items-center">
                            <SvgIcon :name="model.icon" customCss="mr-2 h-5 w-5" />
                            <div class="flex flex-col text-xs">
                                <div class="font-medium text-gray-800 dark:text-gray-100">{{ model.name }}</div>
                                <div class="text-gray-500 dark:text-gray-400">{{ model.description }}</div>
                            </div>
                        </div>
                        <!-- 右侧对号 -->
                        <SvgIcon v-if="model.selected" name="check" customCss="h-3.5 w-3.5 text-[#4d6bfe]" />
                    </div>
                </div>

                <!-- 联网搜索 -->
                <div
                  class="ml-2 flex cursor-pointer items-center justify-center rounded-xl border px-2.5 py-1.5 transition-colors"
                :class="isNetworkSearchSelected
                  ? 'border-[#c7d6fb] bg-[#edf2ff] hover:bg-[#e2ebff] dark:border-[#3f5cf0] dark:bg-[#26345e] dark:hover:bg-[#2d3d6b]'
                  : 'border-gray-200 hover:border-[#4d6bfe]/30 hover:bg-[#f5f7ff] dark:border-gray-600 dark:hover:border-[#6d8bff]/50 dark:hover:bg-[#2a313c]'"
                @click="toggleNetworkSearch">
                    <SvgIcon name="network" customCss="mr-1.5 h-5 w-5" :class="isNetworkSearchSelected ? 'text-[#4D6BFE] dark:text-[#8fa6ff]' : 'text-gray-500 dark:text-gray-400'" />
                    <span class="mr-1 text-xs font-medium" :class="isNetworkSearchSelected ? 'text-[#4D6BFE] dark:text-[#8fa6ff]' : 'text-gray-700 dark:text-gray-200'">联网搜索</span>
                </div>
            </div>

            <div class="grow"></div>

            <!-- 发送按钮：未输入时 hover 提示“请输入你的问题”（禁用按钮需外套 span 才能触发） -->
            <a-tooltip placement="top" :title="hasContent ? '' : '请输入你的问题'">
              <span class="inline-flex">
                <button class="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-[#4d6bfe] to-[#2e6bff] text-white shadow-md shadow-[#4d6bfe]/30 transition-all
                      hover:from-[#3f5cf0] hover:to-[#2560f0] active:scale-95
                      disabled:opacity-40
                      disabled:cursor-not-allowed disabled:shadow-none"
                      :disabled="loading || !hasContent"
                      @click="handleSendMessage"
                      >
                  <SvgIcon name="up-arrow" customCss="h-5 w-5 text-white"></SvgIcon>
                </button>
              </span>
            </a-tooltip>
        </div>
        </div>

        <!-- 提示：严格显示在输入框卡片下方 -->
        <div class="mt-2 px-4 text-center text-xs text-gray-400">
          内容由 AI 生成，请仔细甄别
        </div>
    </div>
</template>

<script setup>
import SvgIcon from '@/components/SvgIcon.vue'
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chatStore'

// 获取 chat store
const chatStore = useChatStore()
// 模型列表
const models = computed(() => chatStore.models)

// 输入框引用
const textareaRef = ref(null)
// 下拉框容器引用
const selectRef = ref(null)
// 下拉菜单状态
const isModelDropdownOpen = ref(false)
// 当前选择的模型，使用 store 中的选中模型
const currSelectedModel = computed(() => chatStore.selectedModel)
// 选择模型
const selectModel = (model) => {
  // 更新 store 中的选中模型
  chatStore.updateSelectedModel(model);
  
  // 关闭下拉菜单
  isModelDropdownOpen.value = false;
}

// 是否启用联网搜索，使用 store 中的状态
const isNetworkSearchSelected = computed(() => chatStore.isNetworkSearchSelected)

// 切换联网搜索选中状态
const toggleNetworkSearch = () => {
  // 更新 store 中的联网搜索状态
  chatStore.updateNetworkSearchStatus(!chatStore.isNetworkSearchSelected)
}

// 下拉菜单显示/隐藏
const toggleModelDropdown = () => {
  isModelDropdownOpen.value = !isModelDropdownOpen.value
}

// 点击外部区域关闭下拉菜单
const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isModelDropdownOpen.value = false
  }
}

// 接收父组件传递的属性
const props = defineProps({
  // textarea 中用户输入的用户消息
  modelValue: {
    type: String,
    required: true
  },
  // 是否正在请求中（用于禁用发送）
  loading: {
    type: Boolean,
    default: false
  },
  // 是否展示“模型选择 + 联网搜索”工具行（客服抽屉等简版输入场景可关闭）
  showTools: {
    type: Boolean,
    default: true
  },
})

// 定义 emits
const emit = defineEmits(['update:modelValue', 'sendMessage'])

// 计算属性，用于 v-model 的双向绑定
const userMessage = computed({
  get() {
    return props.modelValue;
  },
  set(value) {
    emit('update:modelValue', value);
  }
})

// 输入是否非空（用于禁用发送按钮）
const hasContent = computed(() => (userMessage.value || '').trim().length > 0)

// textarea 最大高度（超过后内部滚动）
const maxTextareaHeight = 160;

// 输入框自动增高
const autoResize = () => {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, maxTextareaHeight)}px`;
  el.style.overflowY = el.scrollHeight > maxTextareaHeight ? 'auto' : 'hidden';
};

// 清空后复位输入框高度
const resetTextareaHeight = () => {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.overflowY = 'hidden';
};

// 外部修改 modelValue（发送失败回填、父组件清空等）时同步输入框高度
watch(
  () => props.modelValue,
  (value) => {
    nextTick(() => {
      if ((value || '').trim()) {
        autoResize();
      } else {
        resetTextareaHeight();
      }
    });
  },
  { immediate: true }
);

// 挂载时添加事件监听器
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

// 卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// textarea 回车：直接发送；组合输入（中文）/Shift+Enter 则保留换行
const handleEnterKey = (event) => {
  if (event.shiftKey || event.isComposing || event.keyCode === 229) {
    return
  }
  event.preventDefault()
  handleSendMessage()
}

// 处理发送消息
const handleSendMessage = () => {
  // 请求中禁止重复发送
  if (props.loading) {
    return
  }

  const content = (userMessage.value || '').trim()
  // 输入为空（按钮已禁用，此处兜底，比如回车触发）
  if (!content) {
    return
  }

  // 把消息内容与当前选中的模型/联网开关一并交给父组件
  emit('sendMessage', {
    message: content,
    modelName: chatStore.selectedModel?.name ?? 'deepseek-v3',
    networkSearch: chatStore.isNetworkSearchSelected
  })

  // 清空输入框并复位高度
  userMessage.value = '';
  nextTick(resetTextareaHeight);
}
</script>
