package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName("light_group")
public class LightGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;

    private Integer power;

    private Long zoneId;

    private String installationLocation;

    /** 启用状态: 0停用 1启用 */
    private Integer status;

    // ===== 报废归档生命周期 =====

    /** ACTIVE正常 PENDING_REVIEW待复核 RETURNED已退回 ARCHIVED已归档 */
    private String archiveStatus;

    private Integer archiveVersion;

    /** 原始报废理由（首次送审后不再被补充改写） */
    private String scrapReason;

    /** 最近一次补充内容 */
    private String supplementNote;

    private String photoNote;

    private LocalDate effectiveDate;

    private String archiveApplicant;

    private LocalDateTime archiveAppliedAt;

    private String archiveReviewer;

    private String archiveReviewOpinion;

    private LocalDateTime archiveArchivedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 所属分区名称（关联查询字段，非表字段） */
    @TableField(exist = false)
    private String zoneName;

    // ===== 查询时关联字段（非表字段） =====

    /** 完整归档历史轨迹（详情接口填充） */
    @TableField(exist = false)
    private List<LightArchiveRecord> archiveRecords;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public Integer getPower() { return power; }
    public void setPower(Integer power) { this.power = power; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getInstallationLocation() { return installationLocation; }
    public void setInstallationLocation(String installationLocation) { this.installationLocation = installationLocation; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
    public Integer getArchiveVersion() { return archiveVersion; }
    public void setArchiveVersion(Integer archiveVersion) { this.archiveVersion = archiveVersion; }
    public String getScrapReason() { return scrapReason; }
    public void setScrapReason(String scrapReason) { this.scrapReason = scrapReason; }
    public String getSupplementNote() { return supplementNote; }
    public void setSupplementNote(String supplementNote) { this.supplementNote = supplementNote; }
    public String getPhotoNote() { return photoNote; }
    public void setPhotoNote(String photoNote) { this.photoNote = photoNote; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getArchiveApplicant() { return archiveApplicant; }
    public void setArchiveApplicant(String archiveApplicant) { this.archiveApplicant = archiveApplicant; }
    public LocalDateTime getArchiveAppliedAt() { return archiveAppliedAt; }
    public void setArchiveAppliedAt(LocalDateTime archiveAppliedAt) { this.archiveAppliedAt = archiveAppliedAt; }
    public String getArchiveReviewer() { return archiveReviewer; }
    public void setArchiveReviewer(String archiveReviewer) { this.archiveReviewer = archiveReviewer; }
    public String getArchiveReviewOpinion() { return archiveReviewOpinion; }
    public void setArchiveReviewOpinion(String archiveReviewOpinion) { this.archiveReviewOpinion = archiveReviewOpinion; }
    public LocalDateTime getArchiveArchivedAt() { return archiveArchivedAt; }
    public void setArchiveArchivedAt(LocalDateTime archiveArchivedAt) { this.archiveArchivedAt = archiveArchivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public List<LightArchiveRecord> getArchiveRecords() { return archiveRecords; }
    public void setArchiveRecords(List<LightArchiveRecord> archiveRecords) { this.archiveRecords = archiveRecords; }
}
