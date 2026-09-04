<template>
  <!-- 问答文件管理抽屉（全局独立：右上角 settings / 客服抽屉均可打开） -->
  <a-drawer v-model:open="visible" title="客服问答文件管理" placement="right" :width="860" :z-index="1200">
    <div class="flex h-full flex-col gap-4">
      <!-- 条件查询：文件名（模糊）+ 创建时间段 -->
      <div class="flex flex-wrap items-center gap-3">
        <a-input v-model:value="searchForm.fileName" placeholder="文件名称（模糊查询）" allow-clear class="!w-56"
          @pressEnter="handleSearch" />
        <a-range-picker v-model:value="searchForm.dateRange" />
        <a-button type="primary" :icon="h(SearchOutlined)" @click="handleSearch">查询</a-button>
        <a-button :icon="h(RedoOutlined)" @click="handleReset">重置</a-button>
      </div>
      <a-divider class="my-0" />

      <!-- 上传入口：支持文档 + 视频（视频用于测试大文件分片上传） -->
      <div class="flex items-center justify-between">
        <a-button type="primary" @click="fileInputRef?.click()">
          <template #icon>
            <UploadOutlined />
          </template>
          上传文件
        </a-button>
        <span class="text-xs text-gray-400">支持文档 / 视频（大文件走分片上传）</span>
        <input ref="fileInputRef" type="file" class="hidden"
          accept=".md,.markdown,.txt,.text,.doc,.docx,.ppt,.pptx,.pdf,.html,.htm,.mp4,.mov,.avi"
          @change="onFileSelect" />
      </div>

      <a-table
        row-key="id"
        :columns="docColumns"
        :data-source="docRows"
        :loading="docLoading"
        :pagination="docPagination"
        size="middle"
        @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status] || 'default'">
              {{ statusTextMap[record.status] || '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="whitespace-nowrap">
              <a-button type="link" size="small" @click="openEditRemark(record)">编辑</a-button>
              <a-divider type="vertical" />
              <a-popconfirm title="删除该文件？删除后不可恢复。" ok-text="删除" cancel-text="取消" @confirm="handleDeleteDoc(record)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </div>
  </a-drawer>

  <!-- 编辑问答文件弹窗（信息回显，仅备注可编辑） -->
  <a-modal
    v-model:open="editRemarkOpen"
    title="编辑问答文件"
    ok-text="提交"
    cancel-text="取消"
    :z-index="1300"
    :confirm-loading="editRemarkLoading"
    @ok="handleSaveRemark">
    <div class="py-2">
      <a-descriptions :column="2" size="small" bordered class="mb-4">
        <a-descriptions-item label="ID">{{ editingRow?.id }}</a-descriptions-item>
        <a-descriptions-item label="处理状态">
          <a-tag v-if="editingRow" :color="statusColorMap[editingRow.status] || 'default'">
            {{ statusTextMap[editingRow.status] || '未知' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="文件名称" :span="2">{{ editingRow?.fileName }}</a-descriptions-item>
        <a-descriptions-item label="文件大小">{{ editingRow?.fileSize }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ editingRow?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间" :span="2">{{ editingRow?.updateTime }}</a-descriptions-item>
      </a-descriptions>

      <div class="mb-1 text-sm text-gray-600 dark:text-gray-300">备注</div>
      <a-textarea v-model:value="editRemark" :rows="4" :maxlength="500" show-count placeholder="请输入备注信息" />
    </div>
  </a-modal>

  <!-- 上传文件模态框：展示文件信息，确认后开始分片上传 -->
  <a-modal v-model:open="uploadFileInfoModelOpen" width="700px" :centered="true" title="文件上传" :footer="null" :z-index="1400">
    <div class="mt-5"></div>
    <!-- 文件信息 -->
    <a-descriptions :column="1">
      <a-descriptions-item label="文件名">{{ selectedFile?.name }}</a-descriptions-item>
      <a-descriptions-item label="文件大小">{{ selectedFile ? filesize(selectedFile.size) : '' }}</a-descriptions-item>
      <a-descriptions-item label="文件 MD5">
        <a-tag v-if="fileMd5" color="blue">{{ fileMd5 }}</a-tag>
        <div v-else>
          <a-spin size="small" /> 计算中...
        </div>
      </a-descriptions-item>
    </a-descriptions>

    <!-- 上传进度 -->
    <a-card v-if="uploading || uploadProgress > 0" size="small" title="上传进度" class="mt-4">
      <a-progress
        :percent="uploadProgress"
        :status="uploadStatus"
        :stroke-color="{
          '0%': '#108ee9',
          '100%': '#87d068',
        }"
      />
      <a-alert :message="statusText" :type="alertType" show-icon style="margin-top: 16px" />
    </a-card>
    <div class="mt-5"></div>

    <!-- 开始上传：MD5 计算完成后才展示 -->
    <a-button
      v-if="selectedFile && fileMd5"
      type="primary"
      size="large"
      block
      :loading="uploading"
      @click="startUpload">
      <template #icon>
        <UploadOutlined />
      </template>
      {{ uploading ? '上传中...' : '开始上传' }}
    </a-button>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed, watch, h } from 'vue'
import {
  SearchOutlined, RedoOutlined, UploadOutlined,
} from '@ant-design/icons-vue'
import { message as antMessage } from 'ant-design-vue'
import { filesize } from 'filesize'
import SparkMD5 from 'spark-md5'
import { useKnowledgeManageStore } from '@/stores/knowledgeManage'
import {
  findCustomerDocPage, deleteCustomerDoc, updateCustomerDocRemark,
  checkFile, uploadFileChunk, mergeFileChunk,
} from '@/api/customerService'

const store = useKnowledgeManageStore()

// 显隐（v-model 到全局状态）
const visible = computed({
  get: () => store.visible,
  set: (val) => {
    store.visible = val
  },
})

// 每次打开都从第一页加载（覆盖右上角 / 客服抽屉等多个入口）
watch(() => store.visible, (val) => {
  if (val) {
    loadFirstPage()
  }
})

// ===== 分片上传 =====
const fileInputRef = ref(null)
// 是否展示上传文件模态框
const uploadFileInfoModelOpen = ref(false)
// 临时存储待上传的文件
const selectedFile = ref(null)
// 计算出的文件 MD5
const fileMd5 = ref('')
// 分片大小：2MB
const CHUNK_SIZE = 2 * 1024 * 1024

// 选择文件后：保存文件、弹出上传模态框、分片读取计算 MD5
const onFileSelect = (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  // 重置上传进度相关状态（避免上一任务进度/文案残留）
  resetUploadState()
  // 重置上一轮状态
  fileMd5.value = ''
  selectedFile.value = file

  // 弹出上传模态框
  uploadFileInfoModelOpen.value = true

  antMessage.info('开始计算文件 MD5 值...')
  calculateMD5(file)
}

// 分片读取大文件并计算 MD5（避免一次性读入内存导致爆内存）
const calculateMD5 = (file) => {
  // 创建 SparkMD5.ArrayBuffer 对象，用于计算 MD5
  const spark = new SparkMD5.ArrayBuffer()
  // 创建 FileReader 对象，用于读取文件
  const fileReader = new FileReader()

  // 计算分片数
  const chunks = Math.ceil(file.size / CHUNK_SIZE)
  // 当前读取的分片，从 0 开始
  let currentChunk = 0

  // 读完一片数据后，追加到 spark 并继续读下一片
  fileReader.onload = (e) => {
    spark.append(e.target.result)
    currentChunk++
    if (currentChunk < chunks) {
      loadNext()
    } else {
      fileMd5.value = spark.end()
      antMessage.success('MD5 计算完成')
    }
  }

  fileReader.onerror = () => {
    antMessage.error('MD5 计算失败')
  }

  // 读取下一分片（slice 为 ArrayBuffer）
  const loadNext = () => {
    const start = currentChunk * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    fileReader.readAsArrayBuffer(file.slice(start, end))
  }

  loadNext()
}

// 是否正在上传中
const uploading = ref(false)
// 上传进度（0-100）
const uploadProgress = ref(0)
// 上传状态：active / success / exception
const uploadStatus = ref('active')
// 状态提示文字
const statusText = ref('')

// 进度提示类型：异常=error，仅明确 success 才显示绿色，其余（上传中/合并中）一律蓝色 info，
// 避免 progress 到 100 但仍在合并时误显示绿色“成功”
const alertType = computed(() => {
  if (uploadStatus.value === 'exception') return 'error'
  if (uploadStatus.value === 'success') return 'success'
  return 'info'
})

const resetUploadState = () => {
  uploading.value = false
  uploadProgress.value = 0
  uploadStatus.value = 'active'
  statusText.value = ''
}

// 结束上传（成功态短暂展示后自动关闭并复位）
const finishUpload = async () => {
  await loadFirstPage()
  setTimeout(() => {
    uploadFileInfoModelOpen.value = false
    resetUploadState()
    selectedFile.value = null
    fileMd5.value = ''
  }, 1200)
}

// 开始上传：秒传检查 -> 分片上传（跳过已传，支持断点续传） -> 合并
const startUpload = async () => {
  if (!selectedFile.value || !fileMd5.value) return

  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = 'active'
  statusText.value = ''

  try {
    const file = selectedFile.value
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

    // 1. 检查文件是否存在（秒传 / 断点续传）
    statusText.value = '检查文件是否已存在...'
    const checkRes = await checkFile(fileMd5.value)
    const checkData = checkRes?.data?.data

    // 秒传：文件已完整存在
    if (checkRes?.data?.success && checkData?.exists && !checkData?.needUpload) {
      uploadProgress.value = 100
      uploadStatus.value = 'success'
      statusText.value = '文件已存在，秒传成功！'
      antMessage.success('秒传成功！')
      await finishUpload()
      return
    }

    // 断点续传：跳过已上传的分片
    const uploadedChunks = (checkData?.exists && checkData?.needUpload) ? (checkData?.uploadedChunks || []) : []

    // 最大并发数
    const MAX_CONCURRENT = 3
    // 最大重试次数
    const MAX_RETRY = 3

    // 待上传分片队列
    const uploadQueue = []
    for (let i = 0; i < totalChunks; i++) {
      if (uploadedChunks.includes(i)) continue
      uploadQueue.push(i)
    }

    // 成功 / 失败分片索引（数组统计，避免并发下计数原子性问题）
    const successUploads = []
    const failedUploads = []

    statusText.value = `开始上传分片... (待上传: ${uploadQueue.length}/${totalChunks})`

    // 2. 上传单个分片（带指数退避重试）
    const uploadChunkWithRetry = async (chunkIndex, retryCount = 0) => {
      try {
        const start = chunkIndex * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.size)
        const chunk = file.slice(start, end)

        const formData = new FormData()
        formData.append('chunk', chunk)
        formData.append('fileMd5', fileMd5.value)
        formData.append('fileName', file.name)
        formData.append('fileSize', file.size)
        formData.append('chunkNumber', chunkIndex)
        formData.append('totalChunks', totalChunks)

        const res = await uploadFileChunk(formData)
        const body = res?.data
        if (!body || body.success === false) {
          throw new Error(body?.message || `分片 ${chunkIndex} 上传失败`)
        }

        successUploads.push(chunkIndex)
        const uploadedCount = uploadedChunks.length + successUploads.length
        uploadProgress.value = Math.floor((uploadedCount / totalChunks) * 100)
        statusText.value = `上传中... ${uploadedCount}/${totalChunks} 分片`
      } catch (error) {
        // 未达最大重试次数：等待后重试（指数退避，最长 5s）
        if (retryCount < MAX_RETRY) {
          console.warn(`分片 ${chunkIndex} 上传失败，正在重试 (${retryCount + 1}/${MAX_RETRY})...`, error)
          statusText.value = `分片 ${chunkIndex} 上传失败，正在重试 (${retryCount + 1}/${MAX_RETRY})...`
          const delay = Math.min(1000 * Math.pow(2, retryCount), 5000)
          await new Promise((resolve) => setTimeout(resolve, delay))
          return uploadChunkWithRetry(chunkIndex, retryCount + 1)
        }

        console.error(`分片 ${chunkIndex} 上传失败，已达到最大重试次数`, error)
        failedUploads.push(chunkIndex)
      }
    }

    // 并发分批上传：每批 MAX_CONCURRENT 个并发执行
    for (let i = 0; i < uploadQueue.length; i += MAX_CONCURRENT) {
      const batch = uploadQueue.slice(i, i + MAX_CONCURRENT)
      await Promise.all(batch.map((chunkIndex) => uploadChunkWithRetry(chunkIndex)))
    }

    console.log('=== 上传完成统计 ===')
    console.log(`成功上传: ${successUploads.length} 个分片`, successUploads)
    console.log(`失败上传: ${failedUploads.length} 个分片`, failedUploads)
    console.log(`总计应上传: ${uploadQueue.length} 个分片`)

    // 有失败分片则终止合并，交用户重试
    if (failedUploads.length > 0) {
      throw new Error(`有 ${failedUploads.length} 个分片上传失败: ${failedUploads.join(', ')}`)
    }

    // 3. 合并分片
    statusText.value = '正在合并文件...'
    await mergeFileChunk(fileMd5.value, 120000)

    uploadProgress.value = 100
    uploadStatus.value = 'success'
    statusText.value = '上传完成！'
    antMessage.success('文件上传成功！')
    await finishUpload()
  } catch (error) {
    console.error('上传失败:', error)
    uploadStatus.value = 'exception'
    statusText.value = '上传失败: ' + (error?.response?.data?.message || error?.message || '请稍后重试')
    antMessage.error(statusText.value)
  } finally {
    uploading.value = false
  }
}

// ===== 文档分页/查询 =====
const docLoading = ref(false)
const docRows = ref([])

const docColumns = [
  { title: '文件名称', dataIndex: 'fileName', key: 'fileName', ellipsis: true },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120 },
]

// 处理状态：0-上传中 1-待处理 2-向量化中 3-已完成 4-失败
const statusTextMap = { 0: '上传中', 1: '待处理', 2: '向量化中', 3: '已完成', 4: '失败' }
const statusColorMap = { 0: 'default', 1: 'warning', 2: 'processing', 3: 'success', 4: 'error' }

const docPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: false })

// 条件查询表单
const searchForm = reactive({
  fileName: '',
  dateRange: [],
})

// 回到第一页并加载
const loadFirstPage = () => {
  docPagination.current = 1
  loadDocs()
}

const handleSearch = () => loadFirstPage()

const handleReset = () => {
  searchForm.fileName = ''
  searchForm.dateRange = []
  loadFirstPage()
}

// 加载文档分页列表
const loadDocs = async () => {
  docLoading.value = true
  try {
    // 条件查询参数：文件名模糊 + 创建时间段（dayjs -> YYYY-MM-DD）
    const [start, end] = searchForm.dateRange || []
    const params = {
      current: docPagination.current,
      size: docPagination.pageSize,
      fileName: searchForm.fileName.trim() || undefined,
      startDate: start ? start.format('YYYY-MM-DD') : undefined,
      endDate: end ? end.format('YYYY-MM-DD') : undefined,
    }
    const res = await findCustomerDocPage(params)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '加载文档列表失败')
    }
    docRows.value = body.data ?? []
    docPagination.total = Number(body.total || 0)
  } catch (error) {
    console.error('加载文档列表失败:', error)
    antMessage.error(error?.message || '加载文档列表失败')
  } finally {
    docLoading.value = false
  }
}

// 表格分页变化
const onTableChange = (pagination) => {
  docPagination.current = pagination.current
  docPagination.pageSize = pagination.pageSize
  loadDocs()
}

// 删除文档
const handleDeleteDoc = async (record) => {
  try {
    const res = await deleteCustomerDoc(record.id)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '删除失败')
    }
    antMessage.success('删除成功')
    // 当前页删空且非第一页时回退一页
    if (docRows.value.length === 1 && docPagination.current > 1) {
      docPagination.current -= 1
    }
    await loadDocs()
  } catch (error) {
    console.error('删除文档失败:', error)
    antMessage.error(error?.message || '删除失败')
  }
}

// ===== 编辑备注 =====
const editRemarkOpen = ref(false)
const editRemarkLoading = ref(false)
const editRemark = ref('')
const editingRow = ref(null)

const openEditRemark = (record) => {
  editingRow.value = record
  editRemark.value = record?.remark || ''
  editRemarkOpen.value = true
}

const handleSaveRemark = async () => {
  const remark = editRemark.value.trim()
  if (!remark || !editingRow.value) {
    antMessage.warning('备注不能为空')
    return
  }
  editRemarkLoading.value = true
  try {
    const res = await updateCustomerDocRemark(editingRow.value.id, remark)
    const body = res?.data
    if (!body || body.success === false) {
      throw new Error(body?.message || '保存失败')
    }
    antMessage.success('已保存')
    editRemarkOpen.value = false
    await loadDocs()
  } catch (error) {
    console.error('更新备注失败:', error)
    antMessage.error(error?.message || '保存失败，请稍后重试')
  } finally {
    editRemarkLoading.value = false
  }
}
</script>

<style scoped>
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
