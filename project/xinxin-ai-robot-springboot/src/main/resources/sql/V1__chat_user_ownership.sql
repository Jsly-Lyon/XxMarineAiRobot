-- ============================================================
-- 会话归属：为 t_chat 增加 user_id，实现“登录用户只能访问自己的会话”
-- 需手动在目标库（如 robot）执行一次
-- 注意：历史已存在且未设置 user_id 的会话，升级后将不再对任何用户可见，
--       如有需要可自行 UPDATE 分配给对应账号（按业务语义）。
-- ============================================================

ALTER TABLE t_chat ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_t_chat_user_id ON t_chat (user_id);
