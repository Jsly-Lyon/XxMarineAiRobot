<template>
  <a-modal
    :open="open"
    width="520px"
    :centered="true"
    title="下载记录"
    :footer="null"
    @update:open="handleOpenChange">
    <!-- 下载记录列表：后续接入 /download/list 等接口后渲染 records -->
    <div v-if="records.length" class="divide-y divide-gray-100 dark:divide-gray-800">
      <div v-for="item in records" :key="item.id" class="flex items-center justify-between py-3">
        <div class="min-w-0">
          <div class="truncate text-sm text-gray-800 dark:text-gray-200">{{ item.name }}</div>
          <div class="text-xs text-gray-400">{{ item.time }}</div>
        </div>
        <DownloadOutlined class="text-base text-[#4d6bfe]" />
      </div>
    </div>

    <!-- 空态占位 -->
    <div v-else class="flex flex-col items-center justify-center gap-3 py-10 text-gray-400">
      <div
        class="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-[#eef3ff] to-[#e0f7ff] dark:from-[#1e293b] dark:to-[#15243b]">
        <DownloadOutlined class="text-xl text-[#4d6bfe] dark:text-[#8fa6ff]" />
      </div>
      <div class="text-center">
        <p class="text-sm font-medium text-gray-600 dark:text-gray-300">暂无下载记录</p>
        <p class="mt-1 text-xs">文档上传 / 下载功能接入后，这里会展示你的下载历史</p>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'

defineProps({
  open: { type: Boolean, default: false },
})

const emit = defineEmits(['update:open'])

// 下载记录数据（预留：后续从后端接口拉取）
const records = ref([])

const handleOpenChange = (val) => {
  emit('update:open', val)
}
</script>
