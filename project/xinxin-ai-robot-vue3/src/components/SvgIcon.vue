<template>
  <svg aria-hidden="true" :class="customCss">
    <use :href="symbolId" />
  </svg>
</template>

<script setup>
import { defineProps, computed } from 'vue'

/**
 * 定义组件 Props (父组件可以传入的参数)
 * @property {string} [prefix='icon'] - symbol 前缀，用于构建 symbolId
 * @property {string} name - 图标名称（必需），对应 SVG 文件名
 * @property {string} [customCss=''] - 自定义 CSS 类名
 */
const props = defineProps({
  prefix: {
    type: String,
    default: 'icon'
  },
  name: {
    type: String,
    required: true
  },
  customCss: {
    type: String,
    default: ''
  }
})

/**
 * 计算 symbolId
 * @returns {string} 格式为 "#prefix-name" 的字符串
 * @example 当 prefix="icon", name="arrow" 时，返回 "#icon-arrow"
 */
const symbolId = computed(() => `#${props.prefix}-${props.name}`)
</script>
