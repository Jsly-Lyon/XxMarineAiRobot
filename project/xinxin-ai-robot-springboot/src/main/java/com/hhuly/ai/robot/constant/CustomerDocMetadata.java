package com.hhuly.ai.robot.constant;

/**
 * 客服问答文档向量化元数据约定：
 * 入库 Milvus 的每个块都携带 ownerUserId，检索时按“系统内置 or 本人上传”过滤，实现严格隔离。
 *
 * @author: li
 * @date: 2026/9/3
 **/
public final class CustomerDocMetadata {

    private CustomerDocMetadata() {
    }

    /** 归属字段：系统内置文档固定为 0；用户上传文档为对应用户 ID */
    public static final String KEY_OWNER_USER_ID = "ownerUserId";

    /** 系统内置文档的固定 owner（所有登录用户都可检索） */
    public static final long SYSTEM_OWNER_USER_ID = 0L;

    /** 来源类型字段（可选，便于调试/展示） */
    public static final String KEY_SOURCE_TYPE = "sourceType";

    /** 内置来源标记 */
    public static final String SOURCE_TYPE_BUILTIN = "BUILTIN";

    /** 用户上传文件的登记 ID（用于溯源与按文件删除） */
    public static final String KEY_MD_STORAGE_ID = "mdStorageId";
}
