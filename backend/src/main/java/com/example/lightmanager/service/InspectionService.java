package com.example.lightmanager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.dto.InspectionBatchCreateDTO;
import com.example.lightmanager.dto.InspectionBatchPageRequestDTO;
import com.example.lightmanager.dto.InspectionItemSaveDTO;
import com.example.lightmanager.entity.InspectionBatch;

public interface InspectionService {

    /** 创建巡检批次：选择分区，按当前灯组生成巡检条目并固化分区快照 */
    InspectionBatch createBatch(InspectionBatchCreateDTO request);

    /** 批次分页 */
    IPage<InspectionBatch> getBatchPage(InspectionBatchPageRequestDTO request);

    /** 批次详情（含巡检条目） */
    InspectionBatch getBatchDetail(Long batchId);

    /** 暂存巡检结果（可部分登记、异常可暂不填说明，之后继续补录） */
    InspectionBatch saveItems(Long batchId, InspectionItemSaveDTO request);

    /** 提交批次：校验完整性，冻结原始结果，生成异常清单 */
    InspectionBatch submitBatch(Long batchId, String operator);
}
