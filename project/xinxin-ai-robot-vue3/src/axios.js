import axios from "axios";
import { useAuthStore } from "@/stores/auth";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: "/api", // 你的 API 基础 URL
    timeout: 7000, // 请求超时时间
})

// 请求拦截器：统一携带登录 token
// 注意：axios 在模块作用域初始化，这里必须在回调内部再调用 useAuthStore()
instance.interceptors.request.use((config) => {
    const auth = useAuthStore()
    if (auth.token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${auth.token}`;
    }
    return config;
});

// 响应拦截器：登录态失效时清空并引导重新登录
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.status === 401) {
            const auth = useAuthStore();
            auth.clearAuth();
            auth.openAuthDialog('login');
        }
        return Promise.reject(error);
    }
);

// 暴露出去
export default instance;
