<template>
  <!-- 动态渲染组件 -->
  <router-view></router-view>
  <!-- 全局登录/注册弹窗 -->
  <AuthDialog />
</template>

<script setup>
import AuthDialog from '@/components/AuthDialog.vue'
</script>
