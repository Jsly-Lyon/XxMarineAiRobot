import axios from "@/axios";
import { token as authToken, clearAuth, openAuthDialog } from "@/store/auth";

// 新建对话
export function newChat(message) {
    return axios.post("/chat/new", {message})
}

// 查询某会话的历史消息（后端按时间升序返回）
export function findChatHistoryMessageList({ chatId, current = 1, size = 200 } = {}) {
    return axios.post("/chat/message/list", { chatId, current, size })
}

// 查询历史会话列表（侧边栏用）
export function findChatHistoryPageList({ current = 1, size = 100 } = {}) {
    return axios.post("/chat/list", { current, size })
}

/**
 * 流式对话（SSE，走 POST）。
 * 后端 POST /chat/completion 返回 text/event-stream，每个事件载荷形如 {"v":"文本块"}，
 * 结束或收到 [DONE] 时回调 onDone。主动取消（signal.abort()）不属于错误。
 */
export async function streamChatCompletion(body, { signal, onData, onDone, onError } = {}) {
    try {
        // SSE 走原生 fetch（非 axios），需要手动携带登录 token
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
        }
        if (authToken.value) {
            headers.Authorization = `Bearer ${authToken.value}`
        }

        const res = await fetch('/api/chat/completion', {
            method: 'POST',
            headers,
            body: JSON.stringify({
                temperature: 0.7,
                networkSearch: false,
                ...body,
            }),
            signal,
        })

        // 登录态失效：清空并引导重新登录
        if (res.status === 401) {
            clearAuth()
            openAuthDialog('login')
            throw new Error('登录已过期，请重新登录')
        }

        if (!res.ok || !res.body) {
            throw new Error(`HTTP ${res.status} ${res.statusText}`)
        }

        // 后端业务异常（如“此对话不存在”）返回 application/json，解析并抛出真实原因
        const contentType = res.headers.get('content-type') || ''
        if (res.ok && contentType.includes('application/json')) {
            const body = await res.json().catch(() => null)
            if (body && body.success === false) {
                throw new Error(body.message || '请求失败')
            }
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        let ended = false

        // 解析单个 SSE 事件：取出其中的 data: 行内容
        const dispatch = (eventText) => {
            const payloads = eventText
                .split('\n')
                .filter((line) => line.startsWith('data:'))
                .map((line) => line.slice(5).trim())
                .filter(Boolean)

            for (const payload of payloads) {
                if (payload === '[DONE]') {
                    ended = true
                    return
                }
                onData && onData(payload)
            }
        }

        const feed = (chunk) => {
            buffer += chunk.replace(/\r\n/g, '\n')
            let boundary
            while (!ended && (boundary = buffer.indexOf('\n\n')) !== -1) {
                const eventText = buffer.slice(0, boundary)
                buffer = buffer.slice(boundary + 2)
                dispatch(eventText)
            }
        }

        while (!ended) {
            const { done, value } = await reader.read()
            if (done) break
            feed(decoder.decode(value, { stream: true }))
        }

        if (!ended && buffer) {
            dispatch(buffer) // 处理流的残余内容
        }

        onDone && onDone()
    } catch (error) {
        // 主动取消（AbortController）不算错误
        if (error && error.name === 'AbortError') {
            return
        }
        onError ? onError(error) : console.error('流式对话请求失败：', error)
    }
}
