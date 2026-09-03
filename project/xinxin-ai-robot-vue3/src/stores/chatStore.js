import { defineStore } from 'pinia'
import { ref } from 'vue'

// 创建store
export const useChatStore = defineStore('chat', () => {

  // 定义模型列表
  const models = ref([
    { id: 1, name: 'deepseek-v3', icon: 'deepseek-logo', description: '更流畅', selected: true },
    { id: 2, name: 'deepseek-r1', icon: 'deepseek-logo', description: '深度思考', selected: false },
  ])

  // 选中的模型，默认为第一个
  const selectedModel = ref(models.value[0])

  // 联网搜索状态，默认为false
  const isNetworkSearchSelected = ref(false)

  // 更新选中的模型
  function updateSelectedModel(model) {
    // 将所有模型的 selected 置为 false
    models.value.forEach(m => {
      m.selected = false;
    });
    
    // 将选中模型的 selected 置为 true
    model.selected = true;
    
    // 更新当前选中的模型
    selectedModel.value = model;
  }

  // 更新联网搜索状态
  function updateNetworkSearchStatus(status) {
    isNetworkSearchSelected.value = status
  }

  // 对外暴露相关变量与方法
  return { models, selectedModel, isNetworkSearchSelected, updateSelectedModel, updateNetworkSearchStatus }
},
{
  // 开启持久化
  persist: true,
})
