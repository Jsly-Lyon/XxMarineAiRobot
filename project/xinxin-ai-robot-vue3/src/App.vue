<template>
  <a-config-provider :locale="locale" :theme="antTheme">
    <!-- 动态渲染组件 -->
    <router-view></router-view>
    <!-- 全局登录/注册弹窗 -->
    <AuthDialog />
    <!-- 智能客服右侧抽屉 -->
    <CustomerServiceDrawer />
    <!-- 问答文件管理右侧抽屉 -->
    <KnowledgeManageDrawer />
    <!-- 抽屉打开时隐藏右上角悬浮入口，避免叠在遮罩上 -->
    <KnowledgeManageEntry v-if="!anyOverlayOpen" />
    <ThemeToggle v-if="!anyOverlayOpen" />
  </a-config-provider>
</template>

<script setup>
import { computed, watch } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import AuthDialog from '@/components/AuthDialog.vue'
import CustomerServiceDrawer from '@/components/CustomerServiceDrawer.vue'
import KnowledgeManageDrawer from '@/components/KnowledgeManageDrawer.vue'
import KnowledgeManageEntry from '@/components/KnowledgeManageEntry.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useCustomerServiceStore } from '@/stores/customerService'
import { useKnowledgeManageStore } from '@/stores/knowledgeManage'
import { isDark, initTheme } from '@/utils/theme'

// 应用初始主题（index.html 已预置，此处兜底同步）
initTheme()

const customerService = useCustomerServiceStore()
const knowledgeManage = useKnowledgeManageStore()
// 是否有抽屉打开（用于隐藏右上角悬浮按钮）
const anyOverlayOpen = computed(() => customerService.visible || knowledgeManage.visible)

// 两个抽屉互斥：同一时间只打开一个，避免遮罩叠层挡交互
watch(
  () => customerService.visible,
  (val) => {
    if (val) knowledgeManage.close()
  }
)
watch(
  () => knowledgeManage.visible,
  (val) => {
    if (val) customerService.close()
  }
)

// 时间类组件中文化
dayjs.locale('zh-cn')

// Ant Design Vue 组件中文语言包
const locale = zhCN

// Ant Design 主题跟随暗/亮模式
const antTheme = computed(() => ({
  algorithm: isDark.value ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: '#4d6bfe',
    borderRadius: 10,
  },
}))
</script>
