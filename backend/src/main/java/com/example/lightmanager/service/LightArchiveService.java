package com.example.lightmanager.service;

import com.example.lightmanager.dto.ArchiveReferenceBlockDTO;
import com.example.lightmanager.dto.ArchiveReviewDTO;
import com.example.lightmanager.dto.ArchiveSubmitDTO;
import com.example.lightmanager.entity.LightGroup;

import java.util.List;

/**
 * 灯组报废归档生命周期：
 * 发起送审 -> 复核退回（补充）-> 再次送审 -> 引用检查通过后确认归档（只读终态）。
 */
public interface LightArchiveService {

    /** 归档详情：当前状态 + 原始理由/补充 + 完整历史轨迹 */
    LightGroup getArchiveDetail(Long lightGroupId);

    /** 归档前引用检查：结果直接呈现在页面上 */
    List<ArchiveReferenceBlockDTO> checkReferences(Long lightGroupId);

    /** 管理员首次发起送审 */
    LightGroup submit(Long lightGroupId, ArchiveSubmitDTO request);

    /** 退回补充后再次送审（原始理由保留，仅追加补充） */
    LightGroup resubmit(Long lightGroupId, ArchiveSubmitDTO request);

    /** 复核人退回并说明原因 */
    LightGroup reviewReturn(Long lightGroupId, ArchiveReviewDTO request);

    /** 复核人确认归档（事务内复检引用，全部解除才生效） */
    LightGroup confirmArchive(Long lightGroupId, ArchiveReviewDTO request);
}
