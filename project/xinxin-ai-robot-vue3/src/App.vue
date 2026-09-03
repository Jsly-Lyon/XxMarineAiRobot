<template>
  <a-config-provider :theme="antTheme">
    <!-- 动态渲染组件 -->
    <router-view></router-view>
    <!-- 全局登录/注册弹窗 -->
    <AuthDialog />
    <!-- 右上角主题切换 -->
    <ThemeToggle />
  </a-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import AuthDialog from '@/components/AuthDialog.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { isDark, initTheme } from '@/utils/theme'

// 应用初始主题（index.html 已预置，此处兜底同步）
initTheme()

// Ant Design 主题跟随暗/亮模式
const antTheme = computed(() => ({
  algorithm: isDark.value ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: '#4d6bfe',
    borderRadius: 10,
  },
}))
</script>
