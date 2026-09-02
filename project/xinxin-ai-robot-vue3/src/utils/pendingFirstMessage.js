/**
 * 首页 -> 聊天页“首条消息”的隐式传递（基于 sessionStorage，不走 URL，
 * 避免 query 长度限制导致超长消息被截断）。键与当前会话绑定，消费后即清除。
 */

const PENDING_PREFIX = 'pending-chat-';

/** 新建会话后，暂存首条消息（含当时选择的模型/联网开关） */
export function savePendingFirstMessage(chatUuid, payload) {
  if (!chatUuid) return;
  try {
    sessionStorage.setItem(PENDING_PREFIX + chatUuid, JSON.stringify(payload));
  } catch (error) {
    console.error('暂存首条消息失败:', error);
  }
}

/** 打开会话时读取并清除暂存的首条消息；没有则返回 null */
export function consumePendingFirstMessage(chatUuid) {
  if (!chatUuid) return null;
  try {
    const key = PENDING_PREFIX + chatUuid;
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    sessionStorage.removeItem(key);
    return JSON.parse(raw);
  } catch (error) {
    console.warn('读取暂存首条消息失败:', error);
    return null;
  }
}
