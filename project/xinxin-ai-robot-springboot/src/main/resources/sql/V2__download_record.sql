-- ============================================================
-- 下载记录表：上传文件 / AI 导出文件被下载时记录（按用户隔离）
-- 需手动在目标库执行一次（同 V1）
-- ============================================================

CREATE TABLE IF NOT EXISTS t_download_record (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,          -- 归属用户
    source_type VARCHAR(32) NOT NULL,          -- UPLOAD / AI_EXPORT
    file_name   VARCHAR(255) NOT NULL,
    file_path   TEXT        NOT NULL,          -- 服务端文件绝对路径
    file_size   BIGINT,
    source_id   BIGINT,                        -- 关联来源（上传文件的 file_storage.id）
    create_time TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_t_download_record_user ON t_download_record (user_id, create_time DESC);
