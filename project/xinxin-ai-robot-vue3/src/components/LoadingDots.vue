<template>
  <div class="loading-animation" aria-label="正在思考">
    <span class="dot"></span>
    <span class="dot"></span>
    <span class="dot"></span>
  </div>
</template>

<script setup>
// 简单的加载动画组件：AI 等待首块回复时显示三个闪烁圆点
</script>

<style scoped>
.loading-animation {
  display: flex;
  align-items: center;
  padding: 7px 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #9aa6ff;
  margin: 0 4px;
  animation: blink 1.4s infinite ease-in-out both;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 80%, 100% {
    opacity: 0;
  }
  40% {
    opacity: 1;
  }
}
</style>
