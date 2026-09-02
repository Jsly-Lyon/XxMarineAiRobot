import axios from '@/axios'

// 登录，返回 { token }
export function login(data) {
    return axios.post('/auth/login', data)
}

// 注册
export function register(data) {
    return axios.post('/auth/register', data)
}

// 登出
export function logout() {
    return axios.post('/auth/logout')
}

// 获取当前登录用户信息（后端读 Redis 会话缓存，返回 {id, username, nickname, avatar, role}）
export function getUserInfo() {
    return axios.get('/auth/info')
}
