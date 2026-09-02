import axios from "axios";
import { token as authToken, clearAuth, openAuthDialog } from "@/store/auth";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: "/api", // 你的 API 基础 URL
    timeout: 7000, // 请求超时时间
})

// 请求拦截器：统一携带登录 token
instance.interceptors.request.use((config) => {
    if (authToken.value) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${authToken.value}`;
    }
    return config;
});

// 响应拦截器：登录态失效时清空并引导重新登录
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.status === 401) {
            clearAuth();
            openAuthDialog('login');
        }
        return Promise.reject(error);
    }
);

// 暴露出去
export default instance;

