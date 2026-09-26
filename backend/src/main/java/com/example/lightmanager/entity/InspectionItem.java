package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("inspection_item")
public class InspectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long lightGroupId;

    private String groupCode;

    /** NORMAL正常 EXTINGUISHED熄灭 FLICKER频闪 DIM亮度不足 */
    private String resultType;

    private String description;

    private Long snapshotZoneId;

    private String snapshotZoneName;

    private String snapshotLocation;

    private LocalDateTime recordedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getSnapshotZoneId() { return snapshotZoneId; }
    public void setSnapshotZoneId(Long snapshotZoneId) { this.snapshotZoneId = snapshotZoneId; }
    public String getSnapshotZoneName() { return snapshotZoneName; }
    public void setSnapshotZoneName(String snapshotZoneName) { this.snapshotZoneName = snapshotZoneName; }
    public String getSnapshotLocation() { return snapshotLocation; }
    public void setSnapshotLocation(String snapshotLocation) { this.snapshotLocation = snapshotLocation; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
