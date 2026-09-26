package com.example.lightmanager.mapper;

import com.example.lightmanager.dto.ArchiveReferenceBlockDTO;
import com.example.lightmanager.entity.LightArchiveRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报废归档流程：灯组生命周期 CAS 更新 + 归档历史 + 归档前引用检查。
 */
@Mapper
public interface ArchiveFlowMapper {

    // ===== 生命周期 CAS 更新（id + archive_version + 期望状态，版本自增） =====

    /** 事务内对灯组行加排他锁，串行化归档确认与引用写入，消除 TOCTOU 竞态 */
    com.example.lightmanager.entity.LightGroup selectLightForUpdate(@Param("id") Long id);

    /** 首次送审：ACTIVE -> PENDING_REVIEW，写入原始理由/照片说明/生效日期 */
    int casSubmit(@Param("id") Long id,
                  @Param("version") Integer version,
                  @Param("scrapReason") String scrapReason,
                  @Param("photoNote") String photoNote,
                  @Param("effectiveDate") LocalDate effectiveDate,
                  @Param("operator") String operator,
                  @Param("time") LocalDateTime time);

    /** 再次送审：RETURNED -> PENDING_REVIEW，仅更新补充内容与送审人，原始理由不动 */
    int casResubmit(@Param("id") Long id,
                    @Param("version") Integer version,
                    @Param("supplementNote") String supplementNote,
                    @Param("operator") String operator,
                    @Param("time") LocalDateTime time);

    /** 复核退回：PENDING_REVIEW -> RETURNED */
    int casReturn(@Param("id") Long id,
                  @Param("version") Integer version,
                  @Param("operator") String operator,
                  @Param("reason") String reason,
                  @Param("time") LocalDateTime time);

    /** 确认归档：PENDING_REVIEW -> ARCHIVED */
    int casArchive(@Param("id") Long id,
                   @Param("version") Integer version,
                   @Param("operator") String operator,
                   @Param("opinion") String opinion,
                   @Param("time") LocalDateTime time);

    // ===== 归档历史（同一条链路追加，按时间升序） =====

    void insertRecord(LightArchiveRecord record);

    List<LightArchiveRecord> selectRecordsByLightGroupId(@Param("lightGroupId") Long lightGroupId);

    /** 当前最大复核轮次（无历史返回0） */
    Integer selectMaxRound(@Param("lightGroupId") Long lightGroupId);

    // ===== 归档前引用检查：返回仍指向该灯组的未解除引用 =====

    /** 未关闭（暂存未提交）的巡检批次：批次含该灯组条目且状态为 DRAFT */
    List<ArchiveReferenceBlockDTO> selectOpenInspectionBatches(@Param("lightGroupId") Long lightGroupId);

    /** 未关闭的异常处置记录：指向该灯组且状态非 CLOSED */
    List<ArchiveReferenceBlockDTO> selectOpenAnomalies(@Param("lightGroupId") Long lightGroupId);

    /** 未核销的划转流水：明细指向该灯组且流水 settle_status = OPEN */
    List<ArchiveReferenceBlockDTO> selectOpenTransferLedgers(@Param("lightGroupId") Long lightGroupId);
}
