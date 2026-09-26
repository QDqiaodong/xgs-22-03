package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

@TableName("inspection_anomaly")
public class InspectionAnomaly {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private String batchNo;

    private Long itemId;

    private Long lightGroupId;

    private String groupCode;

    /** EXTINGUISHED熄灭 FLICKER频闪 DIM亮度不足 */
    private String anomalyType;

    private String originalDescription;

    private String supplementDescription;

    private Long snapshotZoneId;

    private String snapshotZoneName;

    private String snapshotLocation;

    /** PENDING_REVIEW待复核 RETURNED已退回 CONFIRMED已确认 PROCESSING处理中 CLOSED已关闭 */
    private String status;

    private String reporter;

    private LocalDateTime reportedAt;

    private String reviewer;

    private String reviewOpinion;

    private LocalDateTime handlingDeadline;

    private LocalDateTime confirmedAt;

    private String handler;

    private String handlingResult;

    private LocalDateTime handledAt;

    private String closer;

    private LocalDateTime closedAt;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ===== 查询时关联的当前分区信息（非表字段） =====
    @TableField(exist = false)
    private Long currentZoneId;

    @TableField(exist = false)
    private String currentZoneName;

    @TableField(exist = false)
    private Boolean zoneChanged;

    @TableField(exist = false)
    private List<InspectionActionLog> logs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }
    public String getSupplementDescription() { return supplementDescription; }
    public void setSupplementDescription(String supplementDescription) { this.supplementDescription = supplementDescription; }
    public Long getSnapshotZoneId() { return snapshotZoneId; }
    public void setSnapshotZoneId(Long snapshotZoneId) { this.snapshotZoneId = snapshotZoneId; }
    public String getSnapshotZoneName() { return snapshotZoneName; }
    public void setSnapshotZoneName(String snapshotZoneName) { this.snapshotZoneName = snapshotZoneName; }
    public String getSnapshotLocation() { return snapshotLocation; }
    public void setSnapshotLocation(String snapshotLocation) { this.snapshotLocation = snapshotLocation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getReviewOpinion() { return reviewOpinion; }
    public void setReviewOpinion(String reviewOpinion) { this.reviewOpinion = reviewOpinion; }
    public LocalDateTime getHandlingDeadline() { return handlingDeadline; }
    public void setHandlingDeadline(LocalDateTime handlingDeadline) { this.handlingDeadline = handlingDeadline; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getHandlingResult() { return handlingResult; }
    public void setHandlingResult(String handlingResult) { this.handlingResult = handlingResult; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getCloser() { return closer; }
    public void setCloser(String closer) { this.closer = closer; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCurrentZoneId() { return currentZoneId; }
    public void setCurrentZoneId(Long currentZoneId) { this.currentZoneId = currentZoneId; }
    public String getCurrentZoneName() { return currentZoneName; }
    public void setCurrentZoneName(String currentZoneName) { this.currentZoneName = currentZoneName; }
    public Boolean getZoneChanged() { return zoneChanged; }
    public void setZoneChanged(Boolean zoneChanged) { this.zoneChanged = zoneChanged; }
    public List<InspectionActionLog> getLogs() { return logs; }
    public void setLogs(List<InspectionActionLog> logs) { this.logs = logs; }
}
