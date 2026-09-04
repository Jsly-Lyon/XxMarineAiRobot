package com.hhuly.ai.robot.model.vo.customerService;

import com.hhuly.ai.robot.model.common.BasePageQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindMarkdownFilePageListReqVO extends BasePageQuery {

    /**
     * 文件名称（模糊查询）
     */
    private String fileName;

    /**
     * 起始日期（按创建时间）
     */
    private LocalDate startDate;

    /**
     * 结束日期（按创建时间，含当天）
     */
    private LocalDate endDate;

}
