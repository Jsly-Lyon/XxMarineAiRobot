package com.hhuly.ai.robot.enums;

import com.hhuly.ai.robot.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),


    // ----------- 业务异常状态码 -----------
    // ----------- 业务异常状态码 -----------
    CHAT_NOT_EXISTED("20000", "此对话不存在"),
    USER_NOT_EXISTED("20001", "用户不存在"),
    USERNAME_OR_PASSWORD_ERROR("20002", "用户名或密码错误"),
    USERNAME_EXISTED("20003", "用户名已存在"),
    NOT_LOGIN("20004", "未登录或登录已过期"),

    MD_FILE_TYPE_NOT_SUPPORT("20010", "仅支持上传 Markdown(.md) 文件"),
    MD_FILE_SIZE_EXCEED("20011", "文件大小超过限制"),
    MD_FILE_NOT_FOUND("20012", "Markdown 问答文件不存在"),
    MD_FILE_EMPTY("20013", "上传文件不能为空"),
    MD_FILE_UPLOAD_FAILED("20014", "文件上传失败"),
    MD_FILE_CANT_DELETE("20015", "正在处理中的 Markdown 问答文件，不允许删除"),

    // 别名：兼容业务侧 MARKDOWN_FILE_* / MD_FILE_NOT_EXISTED 引用（与上述同码）
    MARKDOWN_FILE_NOT_FOUND("20012", "Markdown 问答文件不存在"),
    MARKDOWN_FILE_CANT_DELETE("20015", "正在处理中的 Markdown 问答文件，不允许删除"),
    MD_FILE_NOT_EXISTED("20012", "Markdown 问答文件不存在"),

    UPLOAD_FILE_CANT_EMPTY("20016", "上传文件不能为空"),
    ONLY_SUPPORT_MARKDOWN("20017", "仅支持上传 Markdown(.md) 文件"),
    UPLOAD_FILE_FAILED("20018", "文件上传失败"),
    DOC_TYPE_NOT_SUPPORT("20019", "不支持的文件类型，仅支持 Markdown/TXT/Word/PPT/PDF/HTML"),

    MERGE_CHUNK_NOT_FOUND("20020", "合并的分片文件不存在"),
    CHUNK_NUM_NOT_COMPLETE("20021", "分片数量不完整"),
    FILE_NOT_EXISTED("20022", "文件不存在或无权访问"),
    DOWNLOAD_RECORD_NOT_EXISTED("20023", "下载记录不存在"),
    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

}
