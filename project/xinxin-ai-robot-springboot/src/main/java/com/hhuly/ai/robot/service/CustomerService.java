package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.domain.dos.AiCustomerServiceMdStorageDO;
import com.hhuly.ai.robot.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.hhuly.ai.robot.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import org.springframework.web.multipart.MultipartFile;

/**
 * 智能客服文件服务接口
 *
 * @author: li
 * @date: 2026/9/3
 **/
public interface CustomerService {

    /**
     * 上传 Markdown 问答文件：落盘 + 登记(status=PENDING) + 发布事件(由监听器异步向量化)，返回 fileId
     *
     * @param file md 文件
     * @return fileId
     */
    Response<Long> uploadMarkdownFile(MultipartFile file);

    /**
     * 通用多格式文档上传：md/txt/doc(x)/ppt(x)/pdf/html 等。
     * 落盘 + 登记(status=PENDING) + 发布事件(异步向量化)，返回 fileId。
     *
     * @param file 文档文件
     * @return fileId
     */
    Response<Long> uploadDocument(MultipartFile file);

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
