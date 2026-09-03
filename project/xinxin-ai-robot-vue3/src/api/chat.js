import axios from "@/axios";
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useAuthStore } from "@/stores/auth";

// 新建对话
export function newChat(message) {
    return axios.post("/chat/new", { message })
}

// 查询某会话的历史消息分页列表（后端按时间升序返回）
export function findChatMessagePageList({ chatId, current = 1, size = 200 } = {}) {
    return axios.post("/chat/message/list", { chatId, current, size })
}

// 兼容旧命名
export const findChatHistoryMessageList = findChatMessagePageList

// 查询历史会话列表（侧边栏用）
export function findChatHistoryPageList({ current = 1, size = 100 } = {}) {
    return axios.post("/chat/list", { current, size })
}

// 删除对话（后端会级联删除该会话下的消息与关联记忆）
export function deleteChat(uuid) {
    return axios.post("/chat/delete", { uuid })
}

// 重命名对话摘要（后端按 id + 当前用户做归属校验）
export function renameChat(id, summary) {
    return axios.post("/chat/summary/rename", { id, summary })
}

/**
 * 流式对话（SSE，走 POST，基于 @microsoft/fetch-event-source）。
 *
 * 后端 POST /chat/completion 返回 text/event-stream，每个事件 data 形如 {"v":"文本块"}。
 * 处理约定：
 *  - onData(data)：收到一段原始 data 字符串（由调用方解析 {"v":...}）；
 *  - onDone()：正常结束（收到 [DONE] 或连接关闭）；
 *  - onError(err)：请求失败 / 业务异常 / 登录失效；
 *  - signal：外部 AbortController，可中断当前流（中断不算错误）。
 */
export async function streamChatCompletion(body, { signal, onData, onDone, onError } = {}) {
    const auth = useAuthStore()

    const controller = new AbortController()
    let finished = false

    // 兼容外部传入的取消信号
    if (signal) {
        const handleAbort = () => controller.abort()
        if (signal.aborted) controller.abort()
        else signal.addEventListener('abort', handleAbort, { once: true })
    }

    const finish = (error) => {
        if (finished) return
        finished = true
        controller.abort()
        if (error) {
            onError ? onError(error) : console.error('流式对话请求失败:', error)
        } else {
            onDone && onDone()
        }
    }

    const headers = { 'Content-Type': 'application/json' }
    if (auth.token) {
        headers.Authorization = `Bearer ${auth.token}`
    }

    try {
        await fetchEventSource('/api/chat/completion', {
            method: 'POST',
            headers,
            signal: controller.signal,
            body: JSON.stringify({
                temperature: 0.7,
                networkSearch: false,
                ...body,
            }),
            async onopen(response) {
                // 登录失效：清空登录态并引导重新登录
                if (response.status === 401) {
                    auth.clearAuth()
                    auth.openAuthDialog('login')
                    throw new Error('登录已过期，请重新登录')
                }
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status} ${response.statusText}`)
                }
                // 若后端以 JSON 返回业务异常（如“此对话不存在”），解析真实原因
                const contentType = response.headers.get('content-type') || ''
                if (!contentType.includes('text/event-stream')) {
                    const text = await response.text().catch(() => '')
                    let message = '请求失败'
                    try {
                        const json = JSON.parse(text)
                        if (json && json.success === false) message = json.message || message
                    } catch (error) { /* 忽略非 JSON 响应 */ }
                    throw new Error(message)
                }
            },
            onmessage(msg) {
                const data = (msg.data || '').trim()
                // 结束哨兵：收到即视为正常收尾
                if (data === '[DONE]') {
                    finish()
                    return
                }
                onData && onData(data)
            },
            onclose() {
                finish()
            },
            onerror(error) {
                finish(error instanceof Error ? error : new Error('连接中断'))
                throw error // 抛出以阻止 fetch-event-source 自动重连
            },
        })

        finish()
    } catch (error) {
        // 主动取消（AbortController）不算错误
        if (error && error.name === 'AbortError') {
            return
        }
        finish(error instanceof Error ? error : new Error(String(error)))
    }
}
