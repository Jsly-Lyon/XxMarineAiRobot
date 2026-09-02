import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
// 导入路由
import router from './router'
// 注册 SVG Icon
import 'virtual:svg-icons-register'

const app = createApp(App)

// 应用路由
app.use(router)

app.mount('#app')
