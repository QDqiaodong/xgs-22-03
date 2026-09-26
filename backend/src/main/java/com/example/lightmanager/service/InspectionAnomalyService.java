package com.example.lightmanager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.dto.AnomalyActionDTO;
import com.example.lightmanager.dto.AnomalyPageRequestDTO;
import com.example.lightmanager.entity.InspectionAnomaly;

public interface InspectionAnomalyService {

    /** 异常清单分页（含当前分区与分区变化标记） */
    IPage<InspectionAnomaly> getAnomalyPage(AnomalyPageRequestDTO request);

    /** 异常详情：原始快照 + 当前状态 + 连续处置记录 */
    InspectionAnomaly getAnomalyDetail(Long id);

    /** 在全新事务中读取最新已提交详情（用于并发冲突时回传获胜方写入的最新内容） */
    InspectionAnomaly getAnomalyDetailFresh(Long id);

    /** 复核退回：待复核 -> 已退回 */
    InspectionAnomaly returnAnomaly(Long id, AnomalyActionDTO request);

    /** 巡检人补充说明：已退回 -> 待复核 */
    InspectionAnomaly supplement(Long id, AnomalyActionDTO request);

    /** 复核确认：待复核 -> 已确认（填写处理意见和处理时限） */
    InspectionAnomaly confirm(Long id, AnomalyActionDTO request);

    /** 登记处理结果：已确认 -> 处理中 */
    InspectionAnomaly resolve(Long id, AnomalyActionDTO request);

    /** 关闭：处理中 -> 已关闭（终态，只读） */
    InspectionAnomaly close(Long id, AnomalyActionDTO request);
}
