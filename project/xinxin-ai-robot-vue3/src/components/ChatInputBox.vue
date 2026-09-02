<template>
    <div class="flex w-full flex-col">
        <div class="bg-gray-100 rounded-3xl px-4 py-3 mx-4 border border-gray-200 flex flex-col">
        <textarea placeholder="向瀚海知问提问，开启海洋科研探索"
            class="bg-transparent border-none outline-none w-full text-sm resize-none min-h-[24px]"
            rows="1"
            v-model="userMessage"
            ref="textareaRef"
            @input="autoResize"
            @keydown.enter.exact.prevent="handleSendMessage"></textarea>

         <!-- 下方容器 -->
        <div class="flex mt-3">
            	<div class="flex gap-2 relative">
                <!-- 大模型下拉框 -->
                <div class="border border-gray-300 px-2 py-1 rounded-3xl flex items-center justify-center hover:bg-gray-200 cursor-pointer"
                ref="selectRef"
                @click="toggleModelDropdown">
                
                    <SvgIcon name="deepseek-logo" customCss="w-5 h-5 mr-1.5" />
                    <span class="text-gray-800 text-xs">{{ currSelectedModel.name }}</span>
                    <SvgIcon name="down-arrow" customCss="w-5 h-5 ml-1 text-gray-800 transform transition-transform duration-300"
                    :class="isModelDropdownOpen ? 'rotate-180' : ''" />
                </div>

                <!-- 下拉框菜单：向上展开（输入框常在屏幕底部，向下会被裁切） -->
                <div v-if="isModelDropdownOpen" class="absolute bottom-full left-0 mb-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-20 overflow-hidden">
                    <div v-for="model in models" :key="model.id" 
                    class="px-3 py-2 hover:bg-gray-100 cursor-pointer flex items-center justify-between"
                    @click="selectModel(model)">
                        <div class="flex items-center">
                            <SvgIcon :name="model.icon" customCss="w-5 h-5 mr-2" />
                            <div class="flex flex-col text-xs">
                                <div class="text-gray-800">{{ model.name }}</div>
                                <div class="text-gray-500">{{ model.description }}</div>
                            </div>
                        </div>
                        <!-- 右侧对号 -->
                        <SvgIcon v-if="model.selected" name="check" customCss="w-3 h-3 text-gray-600" />
                    </div>
                </div>

                <!-- 联网搜索 -->
                <div class="ml-3 border px-2 py-1 rounded-3xl flex items-center justify-center cursor-pointer"
                :class="isNetworkSearchSelected ? 'border-[#ceddee] bg-[#DBEAFE] hover:bg-[#C3DAF8]' : 'border-gray-300 hover:bg-gray-200'" 
                @click="toggleNetworkSearch">
                    <SvgIcon name="network" customCss="w-5 h-5 mr-1.5" :class="isNetworkSearchSelected ? 'text-[#4D6BFE]' : 'text-gray-500'" />
                    <span class="text-xs mr-1" :class="isNetworkSearchSelected ? 'text-[#4D6BFE]' : 'text-gray-800'">联网搜索</span>
                </div>
            </div>

            <div class="grow"></div>

            <!-- 发送按钮 -->
            <button class="flex items-center justify-center bg-[#4d6bfe] rounded-full w-8 h-8 border border-[#4d6bfe] hover:bg-[#3b5bef] transition-colors
                    disabled:opacity-50
                    disabled:cursor-not-allowed"
                    :disabled="loading || !hasContent"
                    @click="handleSendMessage"
                    >
                <SvgIcon name="up-arrow" customCss="w-5 h-5 text-white"></SvgIcon>
            </button>
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

// 模型列表
const models = ref([
  { id: 1, name: 'deepseek-v3', icon: 'deepseek-logo', description: "更流畅", selected: true },
  { id: 2, name: 'deepseek-r1', icon: 'deepseek-logo', description: "深度思考", selected: false },
]);

// 输入框引用
const textareaRef = ref(null)
// 下拉框容器引用
const selectRef = ref(null)
// 下拉菜单状态
const isModelDropdownOpen = ref(false)
// 当前选择的模型，默认为第一个 deepseek-v3
const currSelectedModel = ref(models.value[0])
// 是否启用联网搜索
const isNetworkSearchSelected = ref(false)

// 选择模型
const selectModel = (model) => {
  // 将所有模型的 selected 置为 false
  models.value.forEach(m => {
    m.selected = false;
  });
  
  // 将选中模型的 selected 置为 true
  model.selected = true;
  
  // 更新当前选中的模型
  currSelectedModel.value = model;
  
  // 关闭下拉菜单
  isModelDropdownOpen.value = false;
}

// 切换联网搜索选中状态
const toggleNetworkSearch = () => {
    isNetworkSearchSelected.value = !isNetworkSearchSelected.value;
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
    modelName: currSelectedModel.value?.name,
    networkSearch: isNetworkSearchSelected.value,
  })

  // 清空输入框并复位高度
  userMessage.value = '';
  nextTick(resetTextareaHeight);
}
</script>
