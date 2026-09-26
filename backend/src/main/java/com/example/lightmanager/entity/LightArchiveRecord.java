package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 灯组报废归档历史（同一条链路上追加，不可修改）。
 * 快照字段保留操作当时的灯组编号与分区，不被后续台账状态改写。
 */
@TableName("light_archive_record")
public class LightArchiveRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lightGroupId;

    /** 复核轮次：首次送审为1，每次退回后再送审加1 */
    private Integer round;

    /** SUBMIT送审 RETURN退回 RESUBMIT再送审 ARCHIVE确认归档 */
    private String actionType;

    private String scrapReason;

    private String supplementNote;

    private String photoNote;

    private LocalDate effectiveDate;

    /** 复核意见（退回原因 / 确认意见） */
    private String opinion;

    private String operator;

    private LocalDateTime operatedAt;

    /** 操作时灯组编号（快照） */
    private String snapshotGroupCode;

    /** 操作时所属分区ID（快照） */
    private Long snapshotZoneId;

    /** 操作时所属分区名称（快照） */
    private String snapshotZoneName;

    private Integer fromVersion;

    private Integer toVersion;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getScrapReason() { return scrapReason; }
    public void setScrapReason(String scrapReason) { this.scrapReason = scrapReason; }
    public String getSupplementNote() { return supplementNote; }
    public void setSupplementNote(String supplementNote) { this.supplementNote = supplementNote; }
    public String getPhotoNote() { return photoNote; }
    public void setPhotoNote(String photoNote) { this.photoNote = photoNote; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getOperatedAt() { return operatedAt; }
    public void setOperatedAt(LocalDateTime operatedAt) { this.operatedAt = operatedAt; }
    public String getSnapshotGroupCode() { return snapshotGroupCode; }
    public void setSnapshotGroupCode(String snapshotGroupCode) { this.snapshotGroupCode = snapshotGroupCode; }
    public Long getSnapshotZoneId() { return snapshotZoneId; }
    public void setSnapshotZoneId(Long snapshotZoneId) { this.snapshotZoneId = snapshotZoneId; }
    public String getSnapshotZoneName() { return snapshotZoneName; }
    public void setSnapshotZoneName(String snapshotZoneName) { this.snapshotZoneName = snapshotZoneName; }
    public Integer getFromVersion() { return fromVersion; }
    public void setFromVersion(Integer fromVersion) { this.fromVersion = fromVersion; }
    public Integer getToVersion() { return toVersion; }
    public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
