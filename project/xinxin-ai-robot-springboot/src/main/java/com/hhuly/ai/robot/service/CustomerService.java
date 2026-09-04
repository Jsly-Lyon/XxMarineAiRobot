package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.model.vo.customerService.CheckFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.CheckFileRspVO;
import com.hhuly.ai.robot.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.MergeChunkReqVO;
import com.hhuly.ai.robot.model.vo.customerService.UploadChunkReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.hhuly.ai.robot.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;

/**
 * 智能客服文件服务接口
 *
 * @author: li
 * @date: 2026/9/3
 **/
public interface CustomerService {

//    /**
//     * 上传 Markdown 问答文件（单次整文件上传，已废弃，分片上传系列接口见后续小节）
//     */
//    Response<Long> uploadMarkdownFile(MultipartFile file);
//
//    /**
//     * 通用多格式文档上传（单次整文件上传，已废弃，分片上传系列接口见后续小节）
//     */
//    Response<Long> uploadDocument(MultipartFile file);

    /**
     * 分片上传前：检查文件是否存在（秒传 / 断点续传）
     *
     * @param checkFileReqVO 文件 MD5
     * @return 检查结果
     */
    Response<CheckFileRspVO> checkFile(CheckFileReqVO checkFileReqVO);

    /**
     * 文件分片上传（幂等：同一分片重复上传直接成功）
     *
     * @param uploadChunkReqVO 分片内容与元信息
     * @return 处理结果
     */
    Response<?> uploadChunk(UploadChunkReqVO uploadChunkReqVO);

    /**
     * 文件分片合并（校验分片完整后合并，发布事件触发异步向量化）
     *
     * @param mergeChunkReqVO 文件 MD5
     * @return 处理结果
     */
    Response<?> mergeChunk(MergeChunkReqVO mergeChunkReqVO);

    /**
     * 删除 Markdown 问答文件
     * @param deleteMarkdownFileReqVO
     * @return
     */
    Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO);

    /**
     * 分页查询 Markdown 问答文件
     * @param findMarkdownFilePageListReqVO
     * @return
     */
    PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO);

    /**
     * 修改  Markdown 问答文件信息
     * @param updateMarkdownFileReqVO
     * @return
     */
    Response<?> updateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO);

}
