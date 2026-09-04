import axios from "@/axios"

// 客服问答文档分页列表（本人；支持文件名模糊 + 创建时间段条件）
export function findCustomerDocPage({ current = 1, size = 10, fileName, startDate, endDate } = {}) {
    return axios.post("/customer-service/file/list", {
        current,
        size,
        fileName: fileName || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
    })
}

// 删除客服问答文档（本地文件 + 记录 + 向量，仅本人）
export function deleteCustomerDoc(id) {
    return axios.post("/customer-service/file/delete", { id })
}

// 编辑客服问答文档备注（后端 /file/update 仅支持更新 remark）
export function updateCustomerDocRemark(id, remark) {
    return axios.post("/customer-service/file/update", { id, remark })
}

// 检查文件是否存在（秒传 / 断点续传）
export function checkFile(fileMd5) {
    return axios.post("/customer-service/file/check", { fileMd5 })
}

// 上传问答文件分片（multipart/form-data，默认 30s 超时，可自定义）
export function uploadFileChunk(formData, timeout = 30000) {
    return axios.post("/customer-service/file/upload-chunk", formData, {
        timeout,
    })
}

// 合并问答文件分片（合并可能耗时较长，自定义超时）
export function mergeFileChunk(fileMd5, timeout = 30000) {
    return axios.post("/customer-service/file/merge-chunk", { fileMd5 }, {
        timeout,
    })
}
